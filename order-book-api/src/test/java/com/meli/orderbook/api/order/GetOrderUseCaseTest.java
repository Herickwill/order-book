package com.meli.orderbook.api.order;

import com.meli.orderbook.api.command.CommandService;
import com.meli.orderbook.api.command.repository.CommandRow;
import com.meli.orderbook.api.order.repository.OrderRow;
import com.meli.orderbook.shared.order.OrderSide;
import com.meli.orderbook.shared.order.OrderStatus;
import com.meli.orderbook.shared.order.OrderType;
import com.meli.orderbook.shared.order.RejectReason;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetOrderUseCaseTest {

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final GetOrderRequest REQUEST = new GetOrderRequest(ORDER_ID);

    @Mock
    private OrderService orderService;

    @Mock
    private CommandService commandService;

    @InjectMocks
    private GetOrderUseCase getOrderUseCase;

    @Test
    void should_return_the_processed_order_when_it_exists() {
        var processedOrder = new OrderRow(ORDER_ID, "alice", 7L, OrderSide.BUY, OrderType.LIMIT,
                new BigDecimal("10.50"), 10L, 4L, OrderStatus.PARTIALLY_FILLED, null, OffsetDateTime.now());
        when(orderService.findById(ORDER_ID)).thenReturn(Optional.of(processedOrder));

        var response = getOrderUseCase.execute(REQUEST);

        assertThat(response.orderId()).isEqualTo(ORDER_ID);
        assertThat(response.status()).isEqualTo(OrderState.PARTIALLY_FILLED);
        assertThat(response.filledQuantity()).isEqualTo(4L);
        assertThat(response.quantity()).isEqualTo(10L);
        verify(commandService, never()).findNewOrder(any());
    }

    @Test
    void should_expose_the_reject_reason_when_the_order_was_rejected() {
        var rejectedOrder = new OrderRow(ORDER_ID, "carol", 3L, OrderSide.BUY, OrderType.MARKET,
                null, 5L, 0L, OrderStatus.REJECTED, RejectReason.NO_LIQUIDITY, OffsetDateTime.now());
        when(orderService.findById(ORDER_ID)).thenReturn(Optional.of(rejectedOrder));

        var response = getOrderUseCase.execute(REQUEST);

        assertThat(response.status()).isEqualTo(OrderState.REJECTED);
        assertThat(response.rejectReason()).isEqualTo(RejectReason.NO_LIQUIDITY);
        assertThat(response.price()).isNull();
    }

    @Test
    void should_return_received_when_the_order_is_still_in_the_inbox() {
        var pendingCommand = CommandRow.newOrder(
                "dave", "order:r1", ORDER_ID, OrderSide.SELL, OrderType.LIMIT, new BigDecimal("5.00"), 2L);
        when(orderService.findById(ORDER_ID)).thenReturn(Optional.empty());
        when(commandService.findNewOrder(ORDER_ID)).thenReturn(Optional.of(pendingCommand));

        var response = getOrderUseCase.execute(REQUEST);

        assertThat(response.status()).isEqualTo(OrderState.RECEIVED);
        assertThat(response.filledQuantity()).isZero();
        assertThat(response.quantity()).isEqualTo(2L);
        assertThat(response.side()).isEqualTo(OrderSide.SELL);
        assertThat(response.rejectReason()).isNull();
    }

    @Test
    void should_fail_with_not_found_when_the_order_does_not_exist_anywhere() {
        when(orderService.findById(ORDER_ID)).thenReturn(Optional.empty());
        when(commandService.findNewOrder(ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getOrderUseCase.execute(REQUEST)).isInstanceOf(OrderNotFoundException.class);
    }
}
