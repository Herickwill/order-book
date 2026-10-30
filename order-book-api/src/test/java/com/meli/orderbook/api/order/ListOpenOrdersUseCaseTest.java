package com.meli.orderbook.api.order;

import com.meli.orderbook.api.common.Page;
import com.meli.orderbook.api.order.repository.OrderRow;
import com.meli.orderbook.shared.order.OrderSide;
import com.meli.orderbook.shared.order.OrderStatus;
import com.meli.orderbook.shared.order.OrderType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListOpenOrdersUseCaseTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private ListOpenOrdersUseCase listOpenOrdersUseCase;

    @Test
    void should_convert_each_open_order_and_keep_the_pagination() {
        var orderId = UUID.randomUUID();
        var openOrder = new OrderRow(orderId, "bob", 1L, OrderSide.SELL, OrderType.LIMIT,
                new BigDecimal("11.00"), 5L, 1L, OrderStatus.PARTIALLY_FILLED, null, OffsetDateTime.now());
        var page = Page.fromLookahead(List.of(openOrder, openOrder), 2, 1);
        when(orderService.findOpenPage(OrderSide.SELL, "bob", 2, 1)).thenReturn(page);

        var response = listOpenOrdersUseCase.execute(new ListOpenOrdersRequest(OrderSide.SELL, "bob", 2, 1));

        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.orderId()).isEqualTo(orderId);
            assertThat(item.filledQuantity()).isEqualTo(1L);
            assertThat(item.status()).isEqualTo(OrderStatus.PARTIALLY_FILLED);
        });
        assertThat(response.page()).isEqualTo(2);
        assertThat(response.size()).isEqualTo(1);
        assertThat(response.hasNext()).isTrue();
    }

    @Test
    void should_list_all_users_when_the_user_filter_is_blank() {
        when(orderService.findOpenPage(OrderSide.BUY, null, 0, 20)).thenReturn(Page.fromLookahead(List.of(), 0, 20));

        listOpenOrdersUseCase.execute(new ListOpenOrdersRequest(OrderSide.BUY, "  ", null, null));

        verify(orderService).findOpenPage(OrderSide.BUY, null, 0, 20);
    }
}
