package com.meli.orderbook.api.order;

import com.meli.orderbook.api.command.CommandRegistration;
import com.meli.orderbook.api.command.CommandService;
import com.meli.orderbook.api.command.repository.CommandRow;
import com.meli.orderbook.shared.command.CommandStatus;
import com.meli.orderbook.shared.command.CommandType;
import com.meli.orderbook.shared.order.OrderSide;
import com.meli.orderbook.shared.order.OrderType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateOrderUseCaseTest {

    @Mock
    private CommandService commandService;

    @InjectMocks
    private CreateOrderUseCase createOrderUseCase;

    @Test
    void should_register_a_new_order_with_a_prefixed_idempotency_key() {
        var request = new CreateOrderRequest("alice", "c1", OrderSide.BUY, OrderType.LIMIT, new BigDecimal("10.50"), 3L);
        var captor = ArgumentCaptor.forClass(CommandRow.class);
        when(commandService.register(captor.capture()))
                .thenAnswer(call -> CommandRegistration.newlyCreated(call.getArgument(0)));

        var response = createOrderUseCase.execute(request);

        var command = captor.getValue();
        assertThat(command.userId()).isEqualTo("alice");
        assertThat(command.idempotencyKey()).isEqualTo("order:c1");
        assertThat(command.type()).isEqualTo(CommandType.NEW_ORDER);
        assertThat(command.side()).isEqualTo(OrderSide.BUY);
        assertThat(command.orderType()).isEqualTo(OrderType.LIMIT);
        assertThat(command.price()).isEqualByComparingTo("10.50");
        assertThat(command.quantity()).isEqualTo(3L);
        assertThat(command.asset()).isNull();
        assertThat(response.orderId()).isEqualTo(command.orderId());
        assertThat(response.status()).isEqualTo(CommandStatus.RECEIVED);
        assertThat(response.created()).isTrue();
    }

    @Test
    void should_generate_a_new_order_id_when_called_twice() {
        var request = new CreateOrderRequest("alice", "c1", OrderSide.SELL, OrderType.MARKET, null, 1L);
        when(commandService.register(any()))
                .thenAnswer(call -> CommandRegistration.newlyCreated(call.getArgument(0)));

        var first = createOrderUseCase.execute(request);
        var second = createOrderUseCase.execute(request);

        assertThat(first.orderId()).isNotEqualTo(second.orderId());
    }

    @Test
    void should_return_the_original_order_id_when_it_is_a_resend() {
        var request = new CreateOrderRequest("alice", "c1", OrderSide.BUY, OrderType.MARKET, null, 1L);
        var originalOrderId = UUID.randomUUID();
        var original = CommandRow.newOrder(
                "alice", "order:c1", originalOrderId, OrderSide.BUY, OrderType.MARKET, null, 1L);
        when(commandService.register(any())).thenReturn(CommandRegistration.alreadyExisted(original));

        var response = createOrderUseCase.execute(request);

        assertThat(response.created()).isFalse();
        assertThat(response.orderId()).isEqualTo(originalOrderId);
    }
}
