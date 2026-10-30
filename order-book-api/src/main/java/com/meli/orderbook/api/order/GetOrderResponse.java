package com.meli.orderbook.api.order;

import com.meli.orderbook.api.command.repository.CommandRow;
import com.meli.orderbook.api.order.repository.OrderRow;
import com.meli.orderbook.shared.order.OrderSide;
import com.meli.orderbook.shared.order.OrderType;
import com.meli.orderbook.shared.order.RejectReason;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Saída de {@link GetOrderUseCase}: o estado atual de uma ordem.
 *
 * @param orderId        o id da ordem
 * @param userId         o dono da ordem
 * @param side           compra ou venda
 * @param type           {@code LIMIT} ou {@code MARKET}
 * @param price          o preço limite; nulo numa ordem {@code MARKET}
 * @param quantity       a quantidade pedida
 * @param filledQuantity quanto já foi executado
 * @param status         o estado da ordem; {@code RECEIVED} enquanto o Worker não a processou
 * @param rejectReason   o motivo, se e somente se {@code status} é {@code REJECTED}
 * @param createdAt      quando a ordem foi gravada
 */
public record GetOrderResponse(
        UUID orderId,
        String userId,
        OrderSide side,
        OrderType type,
        BigDecimal price,
        long quantity,
        long filledQuantity,
        OrderState status,
        RejectReason rejectReason,
        OffsetDateTime createdAt
) {

    /**
     * Uma ordem já processada pelo Worker.
     *
     * @param order a linha de {@code orders}
     * @return a resposta com o estado gravado pelo Worker
     */
    public static GetOrderResponse from(OrderRow order) {
        return new GetOrderResponse(
                order.id(),
                order.userId(),
                order.side(),
                order.type(),
                order.price(),
                order.quantity(),
                order.filledQuantity(),
                OrderState.of(order.status()),
                order.rejectReason(),
                order.createdAt());
    }

    /**
     * Uma ordem aceita pela API que ainda está no inbox: nada foi executado.
     *
     * @param command o comando {@code NEW_ORDER} que originou a ordem
     * @return a resposta com status {@code RECEIVED}
     */
    public static GetOrderResponse received(CommandRow command) {
        var nothingFilledYet = 0L;

        return new GetOrderResponse(
                command.orderId(),
                command.userId(),
                command.side(),
                command.orderType(),
                command.price(),
                command.quantity(),
                nothingFilledYet,
                OrderState.RECEIVED,
                null,
                command.createdAt());
    }
}
