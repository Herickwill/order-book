package com.meli.orderbook.api.order;

import com.meli.orderbook.api.order.repository.OrderRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void should_request_one_extra_row_and_report_the_next_page() {
        var threeOpenOrders = List.of(openSell("9.50"), openSell("10.00"), openSell("11.00"));
        when(orderRepository.findOpen(OrderSide.SELL, null, 3, 2L)).thenReturn(threeOpenOrders);

        var page = orderService.findOpenPage(OrderSide.SELL, null, 1, 2);

        assertThat(page.items()).extracting(OrderRow::price)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(new BigDecimal("9.50"), new BigDecimal("10.00"));
        assertThat(page.hasNext()).isTrue();
        assertThat(page.page()).isEqualTo(1);
    }

    private static OrderRow openSell(String price) {
        return new OrderRow(UUID.randomUUID(), "bob", 1L, OrderSide.SELL, OrderType.LIMIT,
                new BigDecimal(price), 5L, 0L, OrderStatus.NEW, null, OffsetDateTime.now());
    }
}
