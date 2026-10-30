package com.meli.orderbook.worker.settlement;

import com.meli.orderbook.shared.wallet.Asset;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class WalletTest {

    private static final WalletKey KEY = new WalletKey("alice", Asset.BRL);

    @Test
    void should_move_value_from_available_to_reserved_and_back() {
        var wallet = Wallet.restore(KEY, new BigDecimal("100.00"), BigDecimal.ZERO);

        wallet.reserve(new BigDecimal("30.00"));
        assertThat(wallet.available()).isEqualByComparingTo("70.00");
        assertThat(wallet.reserved()).isEqualByComparingTo("30.00");

        wallet.release(new BigDecimal("10.00"));
        assertThat(wallet.available()).isEqualByComparingTo("80.00");
        assertThat(wallet.reserved()).isEqualByComparingTo("20.00");
    }

    @Test
    void should_fail_instead_of_going_negative_when_reserving_or_spending_too_much() {
        var wallet = Wallet.restore(KEY, new BigDecimal("10.00"), new BigDecimal("5.00"));

        assertThatIllegalStateException().isThrownBy(() -> wallet.reserve(new BigDecimal("10.01")));
        assertThatIllegalStateException().isThrownBy(() -> wallet.spendReserved(new BigDecimal("5.01")));
        assertThatIllegalStateException().isThrownBy(() -> wallet.release(new BigDecimal("5.01")));
    }

    @Test
    void should_reject_zero_and_negative_amounts() {
        var wallet = Wallet.restore(KEY, new BigDecimal("10.00"), BigDecimal.ZERO);

        assertThatIllegalStateException().isThrownBy(() -> wallet.credit(BigDecimal.ZERO));
        assertThatIllegalStateException().isThrownBy(() -> wallet.reserve(new BigDecimal("-1.00")));
    }

    @Test
    void should_accept_a_credit_up_to_exactly_the_column_limit_counting_both_balances() {
        var wallet = Wallet.restore(KEY, new BigDecimal("999999999999998.99"), new BigDecimal("0.50"));

        assertThat(wallet.canCredit(new BigDecimal("0.50"))).isTrue();
        assertThat(wallet.canCredit(new BigDecimal("0.51"))).isFalse();
    }
}
