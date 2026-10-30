package com.meli.orderbook.api.order;

import com.meli.orderbook.shared.order.OrderSide;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ListOpenOrdersRequestTest {

    @Test
    void should_use_the_first_page_and_the_default_size_when_they_are_absent() {
        var request = new ListOpenOrdersRequest(OrderSide.SELL, null, null, null);

        assertThat(request.page()).isZero();
        assertThat(request.size()).isEqualTo(20);
    }

    @Test
    void should_drop_the_user_filter_when_the_user_id_is_blank() {
        var request = new ListOpenOrdersRequest(OrderSide.SELL, " ", 0, 20);

        assertThat(request.userId()).isNull();
    }

    @Test
    void should_keep_the_values_when_they_are_given() {
        var request = new ListOpenOrdersRequest(OrderSide.BUY, "alice", 3, 50);

        assertThat(request.userId()).isEqualTo("alice");
        assertThat(request.page()).isEqualTo(3);
        assertThat(request.size()).isEqualTo(50);
    }
}
