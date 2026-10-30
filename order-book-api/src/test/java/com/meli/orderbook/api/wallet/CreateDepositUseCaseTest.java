package com.meli.orderbook.api.wallet;

import com.meli.orderbook.api.command.CommandRegistration;
import com.meli.orderbook.api.command.CommandService;
import com.meli.orderbook.api.command.repository.CommandRow;
import com.meli.orderbook.shared.command.CommandStatus;
import com.meli.orderbook.shared.command.CommandType;
import com.meli.orderbook.shared.wallet.Asset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateDepositUseCaseTest {

    @Mock
    private CommandService commandService;

    @InjectMocks
    private CreateDepositUseCase createDepositUseCase;

    @Test
    void should_register_a_deposit_with_a_prefixed_idempotency_key() {
        var request = new CreateDepositRequest("alice", "dep-1", Asset.BRL, new BigDecimal("1000.00"));
        var captor = ArgumentCaptor.forClass(CommandRow.class);
        when(commandService.register(captor.capture()))
                .thenAnswer(call -> CommandRegistration.newlyCreated(call.getArgument(0)));

        var response = createDepositUseCase.execute(request);

        var command = captor.getValue();
        assertThat(command.userId()).isEqualTo("alice");
        assertThat(command.idempotencyKey()).isEqualTo("deposit:dep-1");
        assertThat(command.type()).isEqualTo(CommandType.DEPOSIT);
        assertThat(command.asset()).isEqualTo(Asset.BRL);
        assertThat(command.amount()).isEqualByComparingTo("1000.00");
        assertThat(command.orderId()).isNull();
        assertThat(response.depositId()).isEqualTo("dep-1");
        assertThat(response.status()).isEqualTo(CommandStatus.RECEIVED);
        assertThat(response.created()).isTrue();
    }

    @Test
    void should_not_share_a_key_with_an_order_when_both_use_the_same_client_id() {
        var request = new CreateDepositRequest("alice", "abc", Asset.VIB, BigDecimal.ONE);
        var captor = ArgumentCaptor.forClass(CommandRow.class);
        when(commandService.register(captor.capture()))
                .thenAnswer(call -> CommandRegistration.newlyCreated(call.getArgument(0)));

        createDepositUseCase.execute(request);

        assertThat(captor.getValue().idempotencyKey()).isNotEqualTo("order:abc").isEqualTo("deposit:abc");
    }

    @Test
    void should_report_not_created_when_the_deposit_is_a_resend() {
        var request = new CreateDepositRequest("alice", "dep-1", Asset.BRL, BigDecimal.TEN);
        var original = CommandRow.deposit("alice", "deposit:dep-1", Asset.BRL, BigDecimal.TEN);
        when(commandService.register(any())).thenReturn(CommandRegistration.alreadyExisted(original));

        var response = createDepositUseCase.execute(request);

        assertThat(response.created()).isFalse();
        assertThat(response.depositId()).isEqualTo("dep-1");
    }
}
