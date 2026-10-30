package com.meli.orderbook.api.order;

import com.meli.orderbook.shared.order.OrderStatus;

/**
 * Estado de uma ordem como o cliente o vê.
 *
 * <p>É {@link OrderStatus} com um valor a mais, {@code RECEIVED}: a ordem foi
 * aceita pela API e está no inbox, mas o Worker ainda não a processou. Esse
 * valor não existe para o Worker, por isso vive aqui e não no {@code shared}.
 */
public enum OrderState {
    RECEIVED,
    NEW,
    PARTIALLY_FILLED,
    FILLED,
    CANCELLED,
    REJECTED;

    /**
     * O estado de uma ordem já processada pelo Worker. O {@code switch} sem
     * {@code default} faz o compilador apontar um valor novo em {@link OrderStatus}.
     *
     * @param status o estado gravado em {@code orders}
     * @return o estado equivalente
     */
    public static OrderState of(OrderStatus status) {
        return switch (status) {
            case NEW -> NEW;
            case PARTIALLY_FILLED -> PARTIALLY_FILLED;
            case FILLED -> FILLED;
            case CANCELLED -> CANCELLED;
            case REJECTED -> REJECTED;
        };
    }
}
