package com.meli.orderbook.api.order;

import com.meli.orderbook.api.order.repository.OrderRow;
import com.meli.orderbook.shared.order.OrderSide;
import com.meli.orderbook.shared.order.OrderStatus;
import com.meli.orderbook.shared.order.OrderType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Uma ordem aberta no livro, como item de {@link ListOpenOrdersResponse}.
 *
 * @param orderId        o id da ordem
 * @param userId         o dono da ordem
 * @param side           compra ou venda
 * @param type           o tipo; no livro, sempre {@code LIMIT}
 * @param price          o preço limite
 * @param quantity       a quantidade pedida
 * @param filledQuantity quanto já foi executado
 * @param status         {@code NEW} ou {@code PARTIALLY_FILLED}
 * @param createdAt      quando a ordem foi gravada
 */
public record OpenOrderItem(
        UUID orderId,
        String userId,
        OrderSide side,
        OrderType type,
        BigDecimal price,
        long quantity,
        long filledQuantity,
        OrderStatus status,
        OffsetDateTime createdAt
) {

    /**
     * Converte uma linha de {@code orders} em item da listagem.
     *
     * @param order a ordem aberta
     * @return o item
     */
    public static OpenOrderItem from(OrderRow order) {
        return new OpenOrderItem(
                order.id(),
                order.userId(),
                order.side(),
                order.type(),
                order.price(),
                order.quantity(),
                order.filledQuantity(),
                order.status(),
                order.createdAt());
    }
}
