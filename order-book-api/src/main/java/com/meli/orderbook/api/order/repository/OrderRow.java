package com.meli.orderbook.api.order.repository;

import com.meli.orderbook.shared.order.OrderSide;
import com.meli.orderbook.shared.order.OrderStatus;
import com.meli.orderbook.shared.order.OrderType;
import com.meli.orderbook.shared.order.RejectReason;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Uma linha de {@code orders}, coluna por coluna, na ordem do schema.
 *
 * @param id             o id da ordem, gerado pela API
 * @param userId         o dono da ordem
 * @param seq            ordem de chegada atribuída pelo Worker; desempata o mesmo preço
 * @param side           compra ou venda
 * @param type           {@code LIMIT} ou {@code MARKET}
 * @param price          o preço limite; nulo numa ordem {@code MARKET}
 * @param quantity       a quantidade pedida
 * @param filledQuantity quanto já foi executado
 * @param status         o estado da ordem
 * @param rejectReason   o motivo, se e somente se {@code status} é {@code REJECTED}
 * @param createdAt      quando a ordem foi gravada
 */
public record OrderRow(
        UUID id,
        String userId,
        long seq,
        OrderSide side,
        OrderType type,
        BigDecimal price,
        long quantity,
        long filledQuantity,
        OrderStatus status,
        RejectReason rejectReason,
        OffsetDateTime createdAt
) {
}
