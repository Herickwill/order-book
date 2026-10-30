package com.meli.orderbook.api.wallet.web;

import com.meli.orderbook.api.wallet.CreateDepositRequest;
import com.meli.orderbook.api.wallet.CreateDepositResponse;
import com.meli.orderbook.api.wallet.CreateDepositUseCase;
import com.meli.orderbook.api.wallet.GetStatementRequest;
import com.meli.orderbook.api.wallet.GetStatementResponse;
import com.meli.orderbook.api.wallet.GetStatementUseCase;
import com.meli.orderbook.api.wallet.GetWalletRequest;
import com.meli.orderbook.api.wallet.GetWalletResponse;
import com.meli.orderbook.api.wallet.GetWalletUseCase;
import com.meli.orderbook.shared.command.CommandStatus;
import com.meli.orderbook.shared.wallet.Asset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

    @Mock
    private CreateDepositUseCase createDepositUseCase;

    @Mock
    private GetWalletUseCase getWalletUseCase;

    @Mock
    private GetStatementUseCase getStatementUseCase;

    @InjectMocks
    private WalletController walletController;

    @Test
    void should_answer_accepted_when_the_deposit_is_new_and_ok_when_it_is_resent() {
        var request = new CreateDepositRequest("alice", "dep-1", Asset.BRL, BigDecimal.TEN);
        when(createDepositUseCase.execute(request))
                .thenReturn(new CreateDepositResponse("dep-1", CommandStatus.RECEIVED, true))
                .thenReturn(new CreateDepositResponse("dep-1", CommandStatus.RECEIVED, false));

        var first = walletController.deposit(request);
        var second = walletController.deposit(request);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void should_return_the_balances_when_the_wallet_is_requested() {
        var request = new GetWalletRequest("alice");
        var wallet = new GetWalletResponse("alice", List.of());
        when(getWalletUseCase.execute(request)).thenReturn(wallet);

        var response = walletController.getByUserId(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(wallet);
    }

    @Test
    void should_return_ok_with_the_statement_page() {
        var request = new GetStatementRequest("alice", null, null);
        var statement = new GetStatementResponse("alice", List.of(), 0, 20, false);
        when(getStatementUseCase.execute(request)).thenReturn(statement);

        var response = walletController.getStatement(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(statement);
    }
}
