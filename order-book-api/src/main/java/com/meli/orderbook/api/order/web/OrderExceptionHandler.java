package com.meli.orderbook.api.order.web;

import com.meli.orderbook.api.order.OrderNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Erros exclusivos da feature de ordens. Os comuns ficam em
 * {@code ApiExceptionHandler}.
 *
 * <p>Tem precedência sobre o advice global: lá existe um handler de
 * {@link Exception}, e o Spring usa o primeiro advice que tiver qualquer
 * handler compatível, mesmo que outro advice tenha um mais específico.
 */
@RestControllerAdvice(assignableTypes = OrderController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OrderExceptionHandler {

    private static final String ORDER_NOT_FOUND = "Ordem não encontrada.";

    /**
     * Uma ordem que não está em {@code orders} nem no inbox.
     *
     * @param exception a exceção lançada pela consulta
     * @return Problem Detail com status 404
     */
    @ExceptionHandler(OrderNotFoundException.class)
    public ProblemDetail handleOrderNotFound(OrderNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ORDER_NOT_FOUND);
    }
}
