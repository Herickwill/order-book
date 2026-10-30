package com.meli.orderbook.api.order;

import java.util.UUID;

/**
 * Entrada de {@link GetOrderUseCase}, montada a partir de {@code GET /orders/{orderId}}.
 *
 * @param orderId a ordem a consultar, do path
 */
public record GetOrderRequest(UUID orderId) {
}
