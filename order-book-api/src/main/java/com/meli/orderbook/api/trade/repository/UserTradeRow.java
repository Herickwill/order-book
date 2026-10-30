package com.meli.orderbook.api.trade.repository;

import com.meli.orderbook.shared.order.OrderSide;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Um trade do ponto de vista de um usuário: uma linha da consulta de
 * histórico, não da tabela {@code trades}.
 *
 * <p>{@code side} é o lado em que o usuário estava e {@code orderId} é a ordem
 * dele. Num self-trade, o mesmo trade aparece duas vezes, uma como BUY e outra
 * como SELL.
 *
 * @param tradeId   o id do trade
 * @param orderId   a ordem do usuário que participou do trade
 * @param side      o lado do usuário no trade
 * @param price     o preço de execução
 * @param quantity  a quantidade executada
 * @param createdAt quando o trade foi gravado
 */
public record UserTradeRow(
        long tradeId,
        UUID orderId,
        OrderSide side,
        BigDecimal price,
        long quantity,
        OffsetDateTime createdAt
) {
}
