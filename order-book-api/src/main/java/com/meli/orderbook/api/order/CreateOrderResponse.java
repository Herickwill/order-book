package com.meli.orderbook.api.order;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.meli.orderbook.api.command.CommandRegistration;
import com.meli.orderbook.shared.command.CommandStatus;

import java.util.UUID;

/**
 * Saída de {@link CreateOrderUseCase}.
 *
 * <p>{@code status} é o estado do comando no inbox, não o da ordem. O estado
 * da ordem é consultado em {@code GET /orders/{orderId}}.
 *
 * @param orderId o id da ordem; num reenvio, o da ordem original
 * @param status  o estado do comando no inbox
 * @param created {@code true} se esta requisição criou a ordem; fora do JSON,
 *                porque o status HTTP já comunica isso
 */
public record CreateOrderResponse(UUID orderId, CommandStatus status, @JsonIgnore boolean created) {

    /**
     * Monta a resposta a partir do comando registrado no inbox.
     *
     * @param registration o comando registrado no inbox
     * @return a resposta com os dados do comando gravado
     */
    public static CreateOrderResponse from(CommandRegistration registration) {
        var command = registration.command();
        return new CreateOrderResponse(command.orderId(), command.status(), registration.created());
    }
}
