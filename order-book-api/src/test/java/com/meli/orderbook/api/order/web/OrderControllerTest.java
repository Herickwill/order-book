package com.meli.orderbook.api.order.web;

import com.meli.orderbook.api.order.CancelOrderRequest;
import com.meli.orderbook.api.order.CancelOrderResponse;
import com.meli.orderbook.api.order.CancelOrderUseCase;
import com.meli.orderbook.api.order.CreateOrderRequest;
import com.meli.orderbook.api.order.CreateOrderResponse;
import com.meli.orderbook.api.order.CreateOrderUseCase;
import com.meli.orderbook.api.order.GetOrderRequest;
import com.meli.orderbook.api.order.GetOrderResponse;
import com.meli.orderbook.api.order.GetOrderUseCase;
import com.meli.orderbook.api.order.ListOpenOrdersRequest;
import com.meli.orderbook.api.order.ListOpenOrdersResponse;
import com.meli.orderbook.api.order.ListOpenOrdersUseCase;
import com.meli.orderbook.api.order.OrderState;
import com.meli.orderbook.shared.command.CommandStatus;
import com.meli.orderbook.shared.order.OrderSide;
import com.meli.orderbook.shared.order.OrderType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    private static final UUID ORDER_ID = UUID.randomUUID();

    @Mock
    private CreateOrderUseCase createOrderUseCase;

    @Mock
    private CancelOrderUseCase cancelOrderUseCase;

    @Mock
    private GetOrderUseCase getOrderUseCase;

    @Mock
    private ListOpenOrdersUseCase listOpenOrdersUseCase;

    @InjectMocks
    private OrderController orderController;

    private final CreateOrderRequest createRequest =
            new CreateOrderRequest("alice", "c1", OrderSide.BUY, OrderType.LIMIT, BigDecimal.TEN, 1L);

    @Test
    void should_answer_accepted_when_the_order_is_new() {
        var useCaseResponse = new CreateOrderResponse(ORDER_ID, CommandStatus.RECEIVED, true);
        when(createOrderUseCase.execute(createRequest)).thenReturn(useCaseResponse);

        var response = orderController.create(createRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isSameAs(useCaseResponse);
    }

    @Test
    void should_answer_ok_when_the_order_is_a_resend() {
        var useCaseResponse = new CreateOrderResponse(ORDER_ID, CommandStatus.RECEIVED, false);
        when(createOrderUseCase.execute(createRequest)).thenReturn(useCaseResponse);

        var response = orderController.create(createRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void should_answer_accepted_when_the_cancel_is_new_and_ok_when_it_is_resent() {
        var request = new CancelOrderRequest(ORDER_ID, "alice");
        when(cancelOrderUseCase.execute(request))
                .thenReturn(new CancelOrderResponse(ORDER_ID, CommandStatus.RECEIVED, true))
                .thenReturn(new CancelOrderResponse(ORDER_ID, CommandStatus.RECEIVED, false));

        var first = orderController.cancel(request);
        var second = orderController.cancel(request);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void should_return_the_order_when_it_is_found_by_id() {
        var request = new GetOrderRequest(ORDER_ID);
        var order = new GetOrderResponse(ORDER_ID, "alice", OrderSide.BUY, OrderType.LIMIT, BigDecimal.TEN,
                1L, 0L, OrderState.RECEIVED, null, null);
        when(getOrderUseCase.execute(request)).thenReturn(order);

        var response = orderController.getById(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(order);
    }

    @Test
    void should_return_the_page_when_open_orders_are_listed() {
        var request = new ListOpenOrdersRequest(OrderSide.SELL, null, null, null);
        var page = new ListOpenOrdersResponse(List.of(), 0, 20, false);
        when(listOpenOrdersUseCase.execute(request)).thenReturn(page);

        var response = orderController.listOpen(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(page);
    }
}
