package com.meli.orderbook.api.common;

import com.meli.orderbook.api.order.CreateOrderRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    /** Resolve qual método do advice o Spring escolheria para cada exceção. */
    private final ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(ApiExceptionHandler.class);

    private final WebRequest webRequest = new ServletWebRequest(new MockHttpServletRequest());

    /** Mesmas regras de leitura de JSON configuradas no application.properties. */
    private final JsonMapper jsonMapper = JsonMapper.builder()
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .build();

    @Test
    void should_list_errors_sorted_by_field_when_the_request_is_invalid() throws NoSuchMethodException {
        var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "side", "O side é obrigatório."));
        bindingResult.addError(new FieldError("request", "price", "O price deve ser maior que zero."));

        var problem = invalid(bindingResult);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getDetail()).isEqualTo("A requisição tem campos inválidos.");
        assertThat(errorsOf(problem)).containsExactly(
                new ValidationError("price", "O price deve ser maior que zero."),
                new ValidationError("side", "O side é obrigatório."));
    }

    @Test
    void should_explain_the_expected_type_when_a_path_value_does_not_convert() throws NoSuchMethodException {
        var bindingFailure = new FieldError(
                "request", "orderId", "abc", true, new String[]{"typeMismatch"}, null, "Failed to convert");
        bindingFailure.wrap(new TypeMismatchException("abc", UUID.class));
        var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(bindingFailure);

        var problem = invalid(bindingResult);

        assertThat(errorsOf(problem)).containsExactly(
                new ValidationError("orderId", "O orderId deve ser um UUID válido."));
    }

    @Test
    void should_keep_an_error_without_field_when_it_is_reported_on_the_object() throws NoSuchMethodException {
        var bindingResult = new BeanPropertyBindingResult(new Object(), "createOrderRequest");
        bindingResult.addError(new ObjectError("createOrderRequest", "Regra inválida."));

        var problem = invalid(bindingResult);

        assertThat(errorsOf(problem)).containsExactly(new ValidationError("createOrderRequest", "Regra inválida."));
    }

    @Test
    void should_list_accepted_values_when_an_enum_value_does_not_exist() {
        var exception = unreadable("""
                {"userId":"alice","clientOrderId":"c1","side":"COMPRA","type":"LIMIT","price":10,"quantity":1}
                """);

        var problem = unreadableBody(exception);

        assertThat(errorsOf(problem)).containsExactly(new ValidationError("side", "O side deve ser BUY ou SELL."));
    }

    @Test
    void should_point_to_the_field_when_quantity_is_fractional() {
        var exception = unreadable("""
                {"userId":"alice","clientOrderId":"c1","side":"BUY","type":"LIMIT","price":10,"quantity":2.5}
                """);

        var problem = unreadableBody(exception);

        assertThat(errorsOf(problem)).containsExactly(
                new ValidationError("quantity", "O quantity deve ser um número inteiro."));
    }

    @Test
    void should_report_malformed_json_without_a_field() {
        var exception = unreadable("{\"userId\":");

        var problem = unreadableBody(exception);

        assertThat(problem.getDetail()).isEqualTo("O corpo da requisição não é um JSON válido.");
        assertThat(problem.getProperties()).isNullOrEmpty();
    }

    @Test
    void should_answer_service_unavailable_when_the_database_is_down() {
        var problem = handler.handleDatabaseUnavailable(new DataAccessResourceFailureException("banco fora do ar"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(problem.getDetail()).contains("mesma chave");
    }

    @Test
    void should_answer_service_unavailable_when_the_database_fails_transiently() {
        var problem = handler.handleDatabaseUnavailable(new QueryTimeoutException("timeout"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
    }

    @Test
    void should_answer_internal_error_when_the_failure_is_unexpected() {
        var problem = handler.handleUnexpected(new IllegalStateException("invariante"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getDetail()).isEqualTo("Erro inesperado ao processar a requisição.");
    }

    @Test
    void should_route_to_service_unavailable_when_no_connection_can_be_obtained() {
        var handlerMethod = resolver.resolveMethod(new CannotGetJdbcConnectionException("sem conexão"));

        assertThat(handlerMethod).isNotNull();
        assertThat(handlerMethod.getName()).isEqualTo("handleDatabaseUnavailable");
    }

    @Test
    void should_route_to_internal_error_when_a_database_constraint_is_violated() {
        var handlerMethod = resolver.resolveMethod(new DataIntegrityViolationException("CHECK violado"));

        assertThat(handlerMethod).isNotNull();
        assertThat(handlerMethod.getName()).isEqualTo("handleUnexpected");
    }

    private ProblemDetail invalid(BeanPropertyBindingResult bindingResult) throws NoSuchMethodException {
        var exception = new MethodArgumentNotValidException(anyMethodParameter(), bindingResult);
        var response = handler.handleMethodArgumentNotValid(
                exception, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest);
        return bodyOf(response);
    }

    private ProblemDetail unreadableBody(HttpMessageNotReadableException exception) {
        var response = handler.handleHttpMessageNotReadable(
                exception, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest);
        return bodyOf(response);
    }

    /** Faz o Jackson falhar de verdade, como no request real, e embrulha o erro como o Spring faria. */
    private HttpMessageNotReadableException unreadable(String json) {
        try {
            jsonMapper.readValue(json, CreateOrderRequest.class);
        } catch (JacksonException jacksonFailure) {
            return new HttpMessageNotReadableException("corpo inválido", jacksonFailure, mock(HttpInputMessage.class));
        }
        throw new AssertionError("O JSON deveria ser inválido: " + json);
    }

    private static ProblemDetail bodyOf(ResponseEntity<Object> response) {
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        return (ProblemDetail) response.getBody();
    }

    @SuppressWarnings("unchecked")
    private static List<ValidationError> errorsOf(ProblemDetail problem) {
        return (List<ValidationError>) problem.getProperties().get("errors");
    }

    private static MethodParameter anyMethodParameter() throws NoSuchMethodException {
        var method = ApiExceptionHandlerTest.class.getDeclaredMethod("anyHandlerMethod", Object.class);
        return new MethodParameter(method, 0);
    }

    @SuppressWarnings("unused")
    private void anyHandlerMethod(Object argument) {
    }
}
