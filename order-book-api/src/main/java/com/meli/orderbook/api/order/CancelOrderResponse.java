package com.meli.orderbook.api.order;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.meli.orderbook.api.command.CommandRegistration;
import com.meli.orderbook.shared.command.CommandStatus;

import java.util.UUID;

/**
 * Saída de {@link CancelOrderUseCase}.
 *
 * <p>{@code status} é o estado do pedido de cancelamento no inbox. Se a ordem
 * foi de fato cancelada, aparece em {@code GET /orders/{orderId}}.
 *
 * @param orderId a ordem cujo cancelamento foi pedido
 * @param status  o estado do pedido de cancelamento no inbox
 * @param created {@code true} se esta requisição criou o pedido; fora do JSON,
 *                porque o status HTTP já comunica isso
 */
public record CancelOrderResponse(UUID orderId, CommandStatus status, @JsonIgnore boolean created) {

    /**
     * Monta a resposta a partir do pedido registrado no inbox.
     *
     * @param registration o pedido de cancelamento registrado no inbox
     * @return a resposta com os dados do pedido gravado
     */
    public static CancelOrderResponse from(CommandRegistration registration) {
        var command = registration.command();
        return new CancelOrderResponse(command.orderId(), command.status(), registration.created());
    }
}
