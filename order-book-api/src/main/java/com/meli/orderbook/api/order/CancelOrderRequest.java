package com.meli.orderbook.api.order;

import com.meli.orderbook.api.common.ValidUserId;

import java.util.UUID;

/**
 * Entrada de {@link CancelOrderUseCase}, montada a partir de
 * {@code DELETE /orders/{orderId}?userId=}.
 *
 * @param orderId a ordem a cancelar, do path
 * @param userId  quem pede o cancelamento, da query; o Worker só cancela se a
 *                ordem for deste usuário
 */
public record CancelOrderRequest(UUID orderId, @ValidUserId String userId) {
}
