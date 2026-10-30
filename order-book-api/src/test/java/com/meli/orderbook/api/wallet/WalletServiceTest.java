package com.meli.orderbook.api.wallet;

import com.meli.orderbook.api.wallet.repository.StatementEntryRow;
import com.meli.orderbook.api.wallet.repository.StatementRepository;
import com.meli.orderbook.api.wallet.repository.WalletRepository;
import com.meli.orderbook.api.wallet.repository.WalletRow;
import com.meli.orderbook.shared.wallet.Asset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private StatementRepository statementRepository;

    @InjectMocks
    private WalletService walletService;

    @Test
    void should_index_the_wallets_by_asset() {
        var vibWallet = new WalletRow("alice", Asset.VIB, BigDecimal.TEN, BigDecimal.ZERO);
        var brlWallet = new WalletRow("alice", Asset.BRL, BigDecimal.ONE, BigDecimal.ZERO);
        when(walletRepository.findByUserId("alice")).thenReturn(List.of(vibWallet, brlWallet));

        var wallets = walletService.findByAsset("alice");

        assertThat(wallets).containsEntry(Asset.BRL, brlWallet).containsEntry(Asset.VIB, vibWallet);
    }

    @Test
    void should_return_an_empty_map_when_the_user_has_no_wallet() {
        when(walletRepository.findByUserId("zed")).thenReturn(List.of());

        assertThat(walletService.findByAsset("zed")).isEmpty();
    }

    @Test
    void should_ask_for_one_extra_statement_row_from_the_right_offset() {
        var deposit = new StatementEntryRow(1L, OffsetDateTime.now(), null, null, null, null,
                "deposit:d1", Asset.BRL, BigDecimal.TEN);
        when(statementRepository.findByUser("alice", 21, 20L)).thenReturn(List.of(deposit));

        var page = walletService.findStatementPage("alice", 1, 20);

        assertThat(page.items()).containsExactly(deposit);
        assertThat(page.hasNext()).isFalse();
    }
}
