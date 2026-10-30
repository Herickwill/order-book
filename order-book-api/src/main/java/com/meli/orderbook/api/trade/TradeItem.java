package com.meli.orderbook.api.trade;

import com.meli.orderbook.api.trade.repository.UserTradeRow;
import com.meli.orderbook.shared.order.OrderSide;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Um trade do ponto de vista do usuário, como item de {@link ListTradesResponse}.
 *
 * @param tradeId    o id do trade
 * @param orderId    a ordem do usuário que participou do trade
 * @param side       o lado do usuário no trade
 * @param price      o preço de execução
 * @param quantity   a quantidade executada
 * @param executedAt quando o trade aconteceu
 */
public record TradeItem(
        long tradeId,
        UUID orderId,
        OrderSide side,
        BigDecimal price,
        long quantity,
        OffsetDateTime executedAt
) {

    /**
     * Converte uma linha do histórico em item da listagem.
     *
     * @param trade a linha do histórico
     * @return o item
     */
    public static TradeItem from(UserTradeRow trade) {
        return new TradeItem(
                trade.tradeId(),
                trade.orderId(),
                trade.side(),
                trade.price(),
                trade.quantity(),
                trade.createdAt());
    }
}
