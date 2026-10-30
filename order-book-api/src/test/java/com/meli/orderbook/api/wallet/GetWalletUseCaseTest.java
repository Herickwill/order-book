package com.meli.orderbook.api.wallet;

import com.meli.orderbook.api.wallet.repository.WalletRow;
import com.meli.orderbook.shared.wallet.Asset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetWalletUseCaseTest {

    @Mock
    private WalletService walletService;

    @InjectMocks
    private GetWalletUseCase getWalletUseCase;

    @Test
    void should_return_both_assets_zeroed_when_the_user_has_no_wallet() {
        when(walletService.findByAsset("zed")).thenReturn(Map.of());

        var response = getWalletUseCase.execute(new GetWalletRequest("zed"));

        assertThat(response.userId()).isEqualTo("zed");
        assertThat(response.balances()).extracting(BalanceItem::asset).containsExactly(Asset.BRL, Asset.VIB);
        assertThat(response.balances()).allSatisfy(balance -> {
            assertThat(balance.available()).isEqualByComparingTo("0");
            assertThat(balance.reserved()).isEqualByComparingTo("0");
        });
    }

    @Test
    void should_fill_the_missing_asset_with_zero_when_only_one_wallet_exists() {
        var brlWallet = new WalletRow("alice", Asset.BRL, new BigDecimal("900.00"), new BigDecimal("63.00"));
        when(walletService.findByAsset("alice")).thenReturn(Map.of(Asset.BRL, brlWallet));

        var balances = getWalletUseCase.execute(new GetWalletRequest("alice")).balances();

        assertThat(balances.get(0).available()).isEqualByComparingTo("900.00");
        assertThat(balances.get(0).reserved()).isEqualByComparingTo("63.00");
        assertThat(balances.get(1).asset()).isEqualTo(Asset.VIB);
        assertThat(balances.get(1).available()).isEqualByComparingTo("0");
    }

    @Test
    void should_show_vib_without_decimals_and_brl_with_two() {
        var brlWallet = new WalletRow("alice", Asset.BRL, new BigDecimal("1.50"), new BigDecimal("0.00"));
        var vibWallet = new WalletRow("alice", Asset.VIB, new BigDecimal("10.00"), new BigDecimal("2.00"));
        when(walletService.findByAsset("alice")).thenReturn(Map.of(Asset.BRL, brlWallet, Asset.VIB, vibWallet));

        var balances = getWalletUseCase.execute(new GetWalletRequest("alice")).balances();

        var brl = balances.get(0);
        var vib = balances.get(1);
        assertThat(brl.available().scale()).isEqualTo(2);
        assertThat(vib.available().scale()).isZero();
        assertThat(vib.reserved().scale()).isZero();
        assertThat(vib.available()).isEqualByComparingTo("10");
    }
}
