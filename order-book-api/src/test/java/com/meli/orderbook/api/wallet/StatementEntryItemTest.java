package com.meli.orderbook.api.wallet;

import com.meli.orderbook.api.wallet.repository.StatementEntryRow;
import com.meli.orderbook.shared.order.OrderSide;
import com.meli.orderbook.shared.wallet.Asset;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StatementEntryItemTest {

    private static final OffsetDateTime AT = OffsetDateTime.parse("2026-10-29T12:00:00Z");

    @Test
    void should_add_the_amount_to_brl_and_return_the_client_deposit_id_when_the_entry_is_a_brl_deposit() {
        var row = deposit(Asset.BRL, "1212.00");

        var item = StatementEntryItem.from(row);

        assertThat(item.type()).isEqualTo(StatementEntryType.DEPOSIT);
        assertThat(item.brlChange()).isEqualByComparingTo("1212.00");
        assertThat(item.vibChange()).isZero();
        assertThat(item.depositId()).isEqualTo("d1");
        assertThat(item.tradeId()).isNull();
    }

    @Test
    void should_add_whole_units_to_vib_when_the_entry_is_a_vib_deposit() {
        var item = StatementEntryItem.from(deposit(Asset.VIB, "10.00"));

        assertThat(item.vibChange()).isEqualTo(10);
        assertThat(item.brlChange()).isEqualByComparingTo("0.00");
    }

    @Test
    void should_spend_brl_and_receive_vib_when_the_entry_is_a_buy() {
        var orderId = UUID.randomUUID();
        var row = new StatementEntryRow(7L, AT, OrderSide.BUY, orderId, new BigDecimal("100.00"), 5L, null, null, null);

        var item = StatementEntryItem.from(row);

        assertThat(item.type()).isEqualTo(StatementEntryType.BUY);
        assertThat(item.brlChange()).isEqualByComparingTo("-500.00");
        assertThat(item.vibChange()).isEqualTo(5);
        assertThat(item.tradeId()).isEqualTo(7L);
        assertThat(item.orderId()).isEqualTo(orderId);
        assertThat(item.depositId()).isNull();
    }

    @Test
    void should_receive_brl_and_give_vib_when_the_entry_is_a_sell() {
        var row = new StatementEntryRow(8L, AT, OrderSide.SELL, UUID.randomUUID(), new BigDecimal("10.50"), 3L,
                null, null, null);

        var item = StatementEntryItem.from(row);

        assertThat(item.type()).isEqualTo(StatementEntryType.SELL);
        assertThat(item.brlChange()).isEqualByComparingTo("31.50");
        assertThat(item.vibChange()).isEqualTo(-3);
    }

    private static StatementEntryRow deposit(Asset asset, String amount) {
        return new StatementEntryRow(1L, AT, null, null, null, null, "deposit:d1", asset, new BigDecimal(amount));
    }
}
