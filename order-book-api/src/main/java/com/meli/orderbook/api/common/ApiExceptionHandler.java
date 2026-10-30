package com.meli.orderbook.api.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.databind.exc.MismatchedInputException;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Erros comuns a todos os endpoints. Toda resposta de erro é Problem Detail.
 *
 * <p>Estende {@link ResponseEntityExceptionHandler} para que os erros gerados
 * pelo próprio Spring MVC também saiam como Problem Detail: método não
 * suportado (405), content type não suportado (415) e rota inexistente (404).
 * Erros de entrada (400) trazem a lista {@code errors}, com uma frase por regra
 * violada.
 *
 * <p>Fica por último na ordem dos advices: o handler de {@link Exception}
 * captura tudo, e sem essa ordem ele tomaria o lugar dos advices de feature,
 * como o que transforma uma ordem inexistente em 404.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private static final String ERRORS_PROPERTY = "errors";
    private static final String INVALID_REQUEST = "A requisição tem campos inválidos.";
    private static final String INVALID_JSON = "O corpo da requisição não é um JSON válido.";
    private static final String SERVICE_UNAVAILABLE =
            "O serviço está temporariamente indisponível. Tente novamente; "
                    + "reenviar um comando com a mesma chave é seguro.";
    private static final String UNEXPECTED_ERROR = "Erro inesperado ao processar a requisição.";

    private static final Comparator<ValidationError> BY_FIELD_THEN_MESSAGE =
            Comparator.comparing(ValidationError::field).thenComparing(ValidationError::message);

    /**
     * Regra violada no record de request, seja ele lido do corpo JSON ou
     * montado a partir do path e da query. Um valor de path ou query que não
     * converte para o tipo do campo também chega aqui, como erro de binding.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        var errors = exception.getBindingResult().getAllErrors().stream()
                .map(ApiExceptionHandler::toValidationError)
                .toList();

        var problem = badRequest(errors);
        return handleExceptionInternal(exception, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    /**
     * JSON malformado, ou um campo com valor do tipo errado. No segundo caso o
     * Jackson guarda o caminho até o campo, e o erro é reportado nele.
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        var wrongTypeError = findFieldWithWrongType(exception);

        ProblemDetail problem;
        if (wrongTypeError.isPresent()) {
            problem = badRequest(List.of(wrongTypeError.get()));
        } else {
            problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, INVALID_JSON);
        }

        return handleExceptionInternal(exception, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    /**
     * Banco indisponível ou falha transitória: o comando pode não ter sido
     * gravado, e reenviá-lo com a mesma chave é seguro. As outras falhas de
     * acesso a dados, como uma constraint violada ou um SQL inválido, são bugs
     * e não indisponibilidade: caem no 500 de {@link #handleUnexpected(Exception)}.
     *
     * @param exception a falha de acesso ao banco
     * @return Problem Detail com status 503
     */
    @ExceptionHandler({
            DataAccessResourceFailureException.class,
            TransientDataAccessException.class,
            RecoverableDataAccessException.class
    })
    public ProblemDetail handleDatabaseUnavailable(Exception exception) {
        log.error("Banco indisponível", exception);
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE);
    }

    /**
     * Qualquer outra falha: um bug ou uma invariante violada. O cliente recebe
     * uma mensagem genérica; o detalhe fica no log.
     *
     * @param exception a falha inesperada
     * @return Problem Detail com status 500
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception) {
        log.error("Erro inesperado", exception);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_ERROR);
    }

    private static ProblemDetail badRequest(List<ValidationError> errors) {
        var sortedErrors = errors.stream()
                .sorted(BY_FIELD_THEN_MESSAGE)
                .toList();

        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, INVALID_REQUEST);
        problem.setProperty(ERRORS_PROPERTY, sortedErrors);
        return problem;
    }

    /**
     * Um erro de campo traz o nome do campo; um erro de conversão ganha a
     * frase do tipo esperado, no lugar da mensagem técnica do Spring. Um erro
     * de objeto, sem campo, é reportado com o nome do objeto, para não sumir.
     */
    private static ValidationError toValidationError(ObjectError error) {
        if (!(error instanceof FieldError fieldError)) {
            return new ValidationError(error.getObjectName(), error.getDefaultMessage());
        }

        var field = fieldError.getField();

        if (fieldError.isBindingFailure()) {
            var expectedType = expectedTypeOf(fieldError);
            return new ValidationError(field, InvalidValueMessage.of(field, expectedType));
        }

        return new ValidationError(field, fieldError.getDefaultMessage());
    }

    private static Class<?> expectedTypeOf(FieldError bindingFailure) {
        var hasTypeMismatch = bindingFailure.contains(TypeMismatchException.class);
        if (!hasTypeMismatch) {
            return null;
        }

        return bindingFailure.unwrap(TypeMismatchException.class).getRequiredType();
    }

    /**
     * Quando o Jackson não consegue converter um valor (enum inexistente,
     * número fracionário num campo inteiro), a exceção guarda o caminho até o
     * campo e o tipo esperado. Sem esse caminho, o problema é o JSON em si.
     */
    private static Optional<ValidationError> findFieldWithWrongType(HttpMessageNotReadableException exception) {
        var rootCause = exception.getMostSpecificCause();

        if (!(rootCause instanceof MismatchedInputException mismatch)) {
            return Optional.empty();
        }

        var path = mismatch.getPath();
        if (path.isEmpty()) {
            return Optional.empty();
        }

        var field = path.getLast().getPropertyName();
        if (field == null) {
            return Optional.empty();
        }

        var message = InvalidValueMessage.of(field, mismatch.getTargetType());
        return Optional.of(new ValidationError(field, message));
    }
}
