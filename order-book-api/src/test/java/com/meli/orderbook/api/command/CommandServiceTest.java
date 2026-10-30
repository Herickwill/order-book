package com.meli.orderbook.api.command;

import com.meli.orderbook.api.command.repository.CommandRepository;
import com.meli.orderbook.api.command.repository.CommandRow;
import com.meli.orderbook.shared.order.OrderSide;
import com.meli.orderbook.shared.order.OrderType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommandServiceTest {

    @Mock
    private CommandRepository commandRepository;

    @InjectMocks
    private CommandService commandService;

    @Test
    void should_return_created_registration_when_the_row_is_inserted() {
        var newCommand = newOrder("order:c1");
        when(commandRepository.insertIfAbsent(newCommand)).thenReturn(1);

        var registration = commandService.register(newCommand);

        assertThat(registration.created()).isTrue();
        assertThat(registration.command()).isSameAs(newCommand);
        verify(commandRepository, never()).findByIdempotency(anyString(), anyString());
    }

    @Test
    void should_return_the_original_command_when_the_key_already_exists() {
        var resentCommand = newOrder("order:c1");
        var originalCommand = newOrder("order:c1");
        when(commandRepository.insertIfAbsent(resentCommand)).thenReturn(0);
        when(commandRepository.findByIdempotency("alice", "order:c1")).thenReturn(Optional.of(originalCommand));

        var registration = commandService.register(resentCommand);

        assertThat(registration.created()).isFalse();
        assertThat(registration.command()).isSameAs(originalCommand);
    }

    @Test
    void should_fail_when_the_insert_conflicts_but_no_row_is_found() {
        var command = newOrder("order:c1");
        when(commandRepository.insertIfAbsent(command)).thenReturn(0);
        when(commandRepository.findByIdempotency("alice", "order:c1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commandService.register(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("order:c1");
    }

    @Test
    void should_look_up_a_pending_order_by_its_id() {
        var orderId = UUID.randomUUID();
        var pendingCommand = newOrder("order:c1");
        when(commandRepository.findNewOrderByOrderId(orderId)).thenReturn(Optional.of(pendingCommand));

        var found = commandService.findNewOrder(orderId);

        assertThat(found).containsSame(pendingCommand);
    }

    private static CommandRow newOrder(String idempotencyKey) {
        return CommandRow.newOrder(
                "alice", idempotencyKey, UUID.randomUUID(), OrderSide.BUY, OrderType.LIMIT, BigDecimal.TEN, 1L);
    }
}
