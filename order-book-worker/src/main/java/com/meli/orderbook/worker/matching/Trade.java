package com.meli.orderbook.worker.matching;

import com.meli.orderbook.shared.order.OrderSide;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Uma execução entre uma ordem de compra e uma de venda. Vira uma linha de
 * {@code trades}.
 *
 * @param buyOrderId  a ordem de compra
 * @param sellOrderId a ordem de venda
 * @param buyUserId   o comprador
 * @param sellUserId  o vendedor
 * @param price       o preço do maker, sempre
 * @param quantity    a quantidade de VIB executada
 */
public record Trade(
        UUID buyOrderId,
        UUID sellOrderId,
        String buyUserId,
        String sellUserId,
        BigDecimal price,
        long quantity
) {

    /**
     * Cria o trade entre a ordem que chegou (taker) e a que estava no livro
     * (maker), ao preço do maker.
     *
     * @param taker    a ordem que chegou
     * @param maker    a ordem que estava no livro
     * @param quantity a quantidade executada
     * @return o trade com comprador e vendedor definidos pelo lado do taker
     */
    public static Trade between(Order taker, Order maker, long quantity) {
        var takerBuys = taker.side() == OrderSide.BUY;
        var buyOrder = takerBuys ? taker : maker;
        var sellOrder = takerBuys ? maker : taker;

        return new Trade(
                buyOrder.id(),
                sellOrder.id(),
                buyOrder.userId(),
                sellOrder.userId(),
                maker.price(),
                quantity);
    }

    /**
     * O valor pago pelo comprador ao vendedor.
     *
     * @return {@code price * quantity}, exato: o preço tem 2 casas e a quantidade é inteira
     */
    public BigDecimal cost() {
        var units = BigDecimal.valueOf(quantity);
        return price.multiply(units);
    }
}
