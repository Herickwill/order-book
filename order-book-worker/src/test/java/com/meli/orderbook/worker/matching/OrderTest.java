package com.meli.orderbook.worker.matching;

import com.meli.orderbook.shared.order.OrderSide;
import com.meli.orderbook.shared.order.OrderStatus;
import com.meli.orderbook.shared.order.OrderType;
import com.meli.orderbook.shared.order.RejectReason;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class OrderTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-10-29T12:00:00Z");

    @Test
    void should_move_to_partially_filled_and_then_filled_when_executions_add_up_to_the_quantity() {
        var order = limitBuy(5);

        order.fill(2);
        assertThat(order.status()).isEqualTo(OrderStatus.PARTIALLY_FILLED);
        assertThat(order.remaining()).isEqualTo(3);

        order.fill(3);
        assertThat(order.status()).isEqualTo(OrderStatus.FILLED);
        assertThat(order.isOpen()).isFalse();
    }

    @Test
    void should_fail_when_an_execution_exceeds_the_remaining_quantity() {
        var order = limitBuy(5);

        assertThatIllegalStateException().isThrownBy(() -> order.fill(6));
    }

    @Test
    void should_fail_when_a_filled_order_is_executed_cancelled_or_rejected() {
        var order = limitBuy(1);
        order.fill(1);

        assertThatIllegalStateException().isThrownBy(() -> order.fill(1));
        assertThatIllegalStateException().isThrownBy(order::cancel);
        assertThatIllegalStateException().isThrownBy(() -> order.reject(RejectReason.NO_LIQUIDITY));
    }

    @Test
    void should_fail_when_rejecting_an_order_that_already_executed_something() {
        var order = limitBuy(5);
        order.fill(1);

        assertThatIllegalStateException().isThrownBy(() -> order.reject(RejectReason.INSUFFICIENT_FUNDS));
    }

    @Test
    void should_keep_the_filled_quantity_when_a_partial_order_is_cancelled() {
        var order = limitBuy(5);
        order.fill(2);

        order.cancel();

        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.filledQuantity()).isEqualTo(2);
    }

    @Test
    void should_refuse_a_market_order_with_price_and_a_limit_order_without_price() {
        assertThatIllegalArgumentException().isThrownBy(() -> Order.accept(UUID.randomUUID(), "user", 1,
                OrderSide.BUY, OrderType.MARKET, new BigDecimal("10.00"), 1, CREATED_AT));
        assertThatIllegalArgumentException().isThrownBy(() -> Order.accept(UUID.randomUUID(), "user", 1,
                OrderSide.BUY, OrderType.LIMIT, null, 1, CREATED_AT));
    }

    @Test
    void should_refuse_to_restore_an_order_whose_status_contradicts_the_filled_quantity() {
        assertThatIllegalArgumentException().isThrownBy(() -> Order.restore(UUID.randomUUID(), "user", 1,
                OrderSide.BUY, OrderType.LIMIT, new BigDecimal("10.00"), 5, 2, OrderStatus.NEW, CREATED_AT));
        assertThatIllegalArgumentException().isThrownBy(() -> Order.restore(UUID.randomUUID(), "user", 1,
                OrderSide.BUY, OrderType.LIMIT, new BigDecimal("10.00"), 5, 5, OrderStatus.FILLED, CREATED_AT));
    }

    private static Order limitBuy(long quantity) {
        return Order.accept(UUID.randomUUID(), "user", 1, OrderSide.BUY, OrderType.LIMIT,
                new BigDecimal("10.00"), quantity, CREATED_AT);
    }
}
