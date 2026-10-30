package com.meli.orderbook.shared.order;

/**
 * Por que uma ordem foi rejeitada. Só existe quando a ordem está
 * {@code REJECTED}.
 *
 * <ul>
 *   <li>{@code INSUFFICIENT_FUNDS}: o saldo não cobre a reserva, ou a compra
 *       {@code MARKET} não paga nem uma unidade.</li>
 *   <li>{@code NO_LIQUIDITY}: a ordem {@code MARKET} não encontrou nenhuma
 *       ordem do outro lado.</li>
 * </ul>
 */
public enum RejectReason {
    INSUFFICIENT_FUNDS,
    NO_LIQUIDITY
}
