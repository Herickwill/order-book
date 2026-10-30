package com.meli.orderbook.api.wallet.repository;

import com.meli.orderbook.shared.order.OrderSide;
import com.meli.orderbook.shared.wallet.Asset;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StatementEntryRowMapperTest {

    private final StatementEntryRowMapper mapper = new StatementEntryRowMapper();

    @Test
    void should_leave_the_trade_columns_null_when_the_row_is_a_deposit() throws SQLException {
        var resultSet = row(null, null, null, null, "deposit:d1", "VIB", new BigDecimal("10.00"));

        var entry = mapper.mapRow(resultSet, 0);

        assertThat(entry.isDeposit()).isTrue();
        assertThat(entry.side()).isNull();
        assertThat(entry.quantity()).isNull();
        assertThat(entry.asset()).isEqualTo(Asset.VIB);
        assertThat(entry.idempotencyKey()).isEqualTo("deposit:d1");
    }

    @Test
    void should_leave_the_deposit_columns_null_when_the_row_is_a_trade() throws SQLException {
        var orderId = UUID.randomUUID();
        var resultSet = row("SELL", orderId, new BigDecimal("10.00"), 4L, null, null, null);

        var entry = mapper.mapRow(resultSet, 0);

        assertThat(entry.isDeposit()).isFalse();
        assertThat(entry.side()).isEqualTo(OrderSide.SELL);
        assertThat(entry.orderId()).isEqualTo(orderId);
        assertThat(entry.quantity()).isEqualTo(4L);
        assertThat(entry.asset()).isNull();
    }

    private static ResultSet row(
            String side, UUID orderId, BigDecimal price, Long quantity,
            String idempotencyKey, String asset, BigDecimal amount) throws SQLException {

        var resultSet = mock(ResultSet.class);
        when(resultSet.getLong("reference_id")).thenReturn(1L);
        when(resultSet.getObject("occurred_at", OffsetDateTime.class)).thenReturn(OffsetDateTime.now());
        when(resultSet.getString("side")).thenReturn(side);
        when(resultSet.getObject("order_id", UUID.class)).thenReturn(orderId);
        when(resultSet.getBigDecimal("price")).thenReturn(price);
        when(resultSet.getObject("quantity", Long.class)).thenReturn(quantity);
        when(resultSet.getString("idempotency_key")).thenReturn(idempotencyKey);
        when(resultSet.getString("asset")).thenReturn(asset);
        when(resultSet.getBigDecimal("amount")).thenReturn(amount);
        return resultSet;
    }
}
