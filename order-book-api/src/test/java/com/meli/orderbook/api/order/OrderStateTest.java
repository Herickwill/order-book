package com.meli.orderbook.api.order;

import com.meli.orderbook.shared.order.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStateTest {

    @Test
    void should_have_a_state_with_the_same_name_when_converting_each_order_status() {
        for (var status : OrderStatus.values()) {
            assertThat(OrderState.of(status).name()).isEqualTo(status.name());
        }
    }
}
