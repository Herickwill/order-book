package com.meli.orderbook.api.order;

import com.meli.orderbook.api.command.CommandRegistration;
import com.meli.orderbook.api.command.CommandService;
import com.meli.orderbook.api.command.repository.CommandRow;
import com.meli.orderbook.shared.command.CommandType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelOrderUseCaseTest {

    private static final UUID ORDER_ID = UUID.randomUUID();

    @Mock
    private CommandService commandService;

    @InjectMocks
    private CancelOrderUseCase cancelOrderUseCase;

    @Test
    void should_register_a_cancel_with_a_key_derived_from_the_order_id() {
        var captor = ArgumentCaptor.forClass(CommandRow.class);
        when(commandService.register(captor.capture()))
                .thenAnswer(call -> CommandRegistration.newlyCreated(call.getArgument(0)));

        var response = cancelOrderUseCase.execute(new CancelOrderRequest(ORDER_ID, "alice"));

        var command = captor.getValue();
        assertThat(command.userId()).isEqualTo("alice");
        assertThat(command.idempotencyKey()).isEqualTo("cancel:" + ORDER_ID);
        assertThat(command.type()).isEqualTo(CommandType.CANCEL_ORDER);
        assertThat(command.orderId()).isEqualTo(ORDER_ID);
        assertThat(command.side()).isNull();
        assertThat(response.orderId()).isEqualTo(ORDER_ID);
        assertThat(response.created()).isTrue();
    }

    @Test
    void should_produce_the_same_key_when_the_same_order_is_cancelled_twice() {
        var captor = ArgumentCaptor.forClass(CommandRow.class);
        when(commandService.register(captor.capture()))
                .thenAnswer(call -> CommandRegistration.newlyCreated(call.getArgument(0)));

        cancelOrderUseCase.execute(new CancelOrderRequest(ORDER_ID, "alice"));
        cancelOrderUseCase.execute(new CancelOrderRequest(ORDER_ID, "alice"));

        var firstKey = captor.getAllValues().get(0).idempotencyKey();
        var secondKey = captor.getAllValues().get(1).idempotencyKey();
        assertThat(firstKey).isEqualTo(secondKey);
    }

    @Test
    void should_report_not_created_when_the_cancel_is_a_resend() {
        var original = CommandRow.cancelOrder("alice", "cancel:" + ORDER_ID, ORDER_ID);
        when(commandService.register(any()))
                .thenReturn(CommandRegistration.alreadyExisted(original));

        var response = cancelOrderUseCase.execute(new CancelOrderRequest(ORDER_ID, "alice"));

        assertThat(response.created()).isFalse();
    }
}
