package com.meli.orderbook.worker.persistence;

import com.meli.orderbook.shared.order.OrderSide;
import com.meli.orderbook.shared.order.OrderStatus;
import com.meli.orderbook.shared.order.OrderType;
import com.meli.orderbook.shared.wallet.Asset;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Os mapeamentos da reconstrução do estado: ordem aberta e carteira. */
class RowMappersTest {

    @Test
    void should_restore_a_partially_filled_order_with_its_remaining_quantity() throws SQLException {
        var id = UUID.randomUUID();
        var row = orderRow(id, 10, 4, "PARTIALLY_FILLED");

        var order = new OpenOrderRowMapper().mapRow(row, 0);

        assertThat(order.id()).isEqualTo(id);
        assertThat(order.side()).isEqualTo(OrderSide.BUY);
        assertThat(order.type()).isEqualTo(OrderType.LIMIT);
        assertThat(order.status()).isEqualTo(OrderStatus.PARTIALLY_FILLED);
        assertThat(order.remaining()).isEqualTo(6);
        assertThat(order.seq()).isEqualTo(42);
    }

    @Test
    void should_refuse_a_stored_order_whose_status_contradicts_the_filled_quantity() throws SQLException {
        var row = orderRow(UUID.randomUUID(), 10, 4, "NEW");

        assertThatIllegalArgumentException().isThrownBy(() -> new OpenOrderRowMapper().mapRow(row, 0));
    }

    @Test
    void should_restore_a_wallet_with_both_balances() throws SQLException {
        var row = mock(ResultSet.class);
        when(row.getString("user_id")).thenReturn("alice");
        when(row.getString("asset")).thenReturn("VIB");
        when(row.getObject("available", BigDecimal.class)).thenReturn(new BigDecimal("8.00"));
        when(row.getObject("reserved", BigDecimal.class)).thenReturn(new BigDecimal("2.00"));

        var wallet = new WalletRowMapper().mapRow(row, 0);

        assertThat(wallet.userId()).isEqualTo("alice");
        assertThat(wallet.asset()).isEqualTo(Asset.VIB);
        assertThat(wallet.available()).isEqualByComparingTo("8");
        assertThat(wallet.reserved()).isEqualByComparingTo("2");
    }

    private static ResultSet orderRow(UUID id, long quantity, long filled, String status) throws SQLException {
        var row = mock(ResultSet.class);
        when(row.getObject("id", UUID.class)).thenReturn(id);
        when(row.getString("user_id")).thenReturn("alice");
        when(row.getLong("seq")).thenReturn(42L);
        when(row.getString("side")).thenReturn("BUY");
        when(row.getString("type")).thenReturn("LIMIT");
        when(row.getObject("price", BigDecimal.class)).thenReturn(new BigDecimal("10.00"));
        when(row.getLong("quantity")).thenReturn(quantity);
        when(row.getLong("filled_quantity")).thenReturn(filled);
        when(row.getString("status")).thenReturn(status);
        when(row.getObject("created_at", OffsetDateTime.class)).thenReturn(OffsetDateTime.now());
        return row;
    }
}
