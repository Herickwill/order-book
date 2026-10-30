package com.meli.orderbook.worker.persistence;

import com.meli.orderbook.shared.command.CommandType;
import com.meli.orderbook.shared.order.OrderSide;
import com.meli.orderbook.shared.order.OrderType;
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

class CommandRowMapperTest {

    private final CommandRowMapper mapper = new CommandRowMapper();

    @Test
    void should_leave_the_order_columns_null_when_the_command_is_a_deposit() throws SQLException {
        var row = row("DEPOSIT", null, null, null, null, null, "BRL", new BigDecimal("100.00"));

        var command = mapper.mapRow(row, 0);

        assertThat(command.type()).isEqualTo(CommandType.DEPOSIT);
        assertThat(command.asset()).isEqualTo(Asset.BRL);
        assertThat(command.amount()).isEqualByComparingTo("100.00");
        assertThat(command.orderId()).isNull();
        assertThat(command.side()).isNull();
        assertThat(command.quantity()).isNull();
    }

    @Test
    void should_keep_the_price_null_when_the_command_is_a_market_order() throws SQLException {
        var orderId = UUID.randomUUID();
        var row = row("NEW_ORDER", orderId, "SELL", "MARKET", null, 5L, null, null);

        var command = mapper.mapRow(row, 0);

        assertThat(command.type()).isEqualTo(CommandType.NEW_ORDER);
        assertThat(command.orderId()).isEqualTo(orderId);
        assertThat(command.side()).isEqualTo(OrderSide.SELL);
        assertThat(command.orderType()).isEqualTo(OrderType.MARKET);
        assertThat(command.price()).isNull();
        assertThat(command.quantity()).isEqualTo(5L);
        assertThat(command.asset()).isNull();
    }

    private static ResultSet row(
            String type, UUID orderId, String side, String orderType, BigDecimal price, Long quantity,
            String asset, BigDecimal amount) throws SQLException {

        var row = mock(ResultSet.class);
        when(row.getLong("id")).thenReturn(1L);
        when(row.getString("user_id")).thenReturn("alice");
        when(row.getString("type")).thenReturn(type);
        when(row.getObject("order_id", UUID.class)).thenReturn(orderId);
        when(row.getString("side")).thenReturn(side);
        when(row.getString("order_type")).thenReturn(orderType);
        when(row.getObject("price", BigDecimal.class)).thenReturn(price);
        when(row.getObject("quantity", Long.class)).thenReturn(quantity);
        when(row.getString("asset")).thenReturn(asset);
        when(row.getObject("amount", BigDecimal.class)).thenReturn(amount);
        when(row.getObject("created_at", OffsetDateTime.class)).thenReturn(OffsetDateTime.now());
        return row;
    }
}
