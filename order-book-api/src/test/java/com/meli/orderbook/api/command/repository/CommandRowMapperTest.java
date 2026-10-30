package com.meli.orderbook.api.command.repository;

import com.meli.orderbook.shared.command.CommandStatus;
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
    void should_map_order_columns_when_the_row_is_a_new_order() throws SQLException {
        var orderId = UUID.randomUUID();
        var resultSet = row(CommandType.NEW_ORDER, orderId, "BUY", "LIMIT", new BigDecimal("10.50"), 3L, null, null);

        var command = mapper.mapRow(resultSet, 0);

        assertThat(command.type()).isEqualTo(CommandType.NEW_ORDER);
        assertThat(command.orderId()).isEqualTo(orderId);
        assertThat(command.side()).isEqualTo(OrderSide.BUY);
        assertThat(command.orderType()).isEqualTo(OrderType.LIMIT);
        assertThat(command.quantity()).isEqualTo(3L);
        assertThat(command.asset()).isNull();
    }

    @Test
    void should_leave_order_columns_null_when_the_row_is_a_deposit() throws SQLException {
        var resultSet = row(CommandType.DEPOSIT, null, null, null, null, null, "VIB", new BigDecimal("10.00"));

        var command = mapper.mapRow(resultSet, 0);

        assertThat(command.type()).isEqualTo(CommandType.DEPOSIT);
        assertThat(command.side()).isNull();
        assertThat(command.orderType()).isNull();
        assertThat(command.quantity()).isNull();
        assertThat(command.asset()).isEqualTo(Asset.VIB);
        assertThat(command.amount()).isEqualByComparingTo("10");
    }

    private static ResultSet row(CommandType type, UUID orderId, String side, String orderType,
                                 BigDecimal price, Long quantity, String asset, BigDecimal amount) throws SQLException {
        var resultSet = mock(ResultSet.class);
        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getString("user_id")).thenReturn("alice");
        when(resultSet.getString("idempotency_key")).thenReturn("key");
        when(resultSet.getString("type")).thenReturn(type.name());
        when(resultSet.getString("status")).thenReturn(CommandStatus.PROCESSED.name());
        when(resultSet.getObject("order_id", UUID.class)).thenReturn(orderId);
        when(resultSet.getString("side")).thenReturn(side);
        when(resultSet.getString("order_type")).thenReturn(orderType);
        when(resultSet.getBigDecimal("price")).thenReturn(price);
        when(resultSet.getObject("quantity", Long.class)).thenReturn(quantity);
        when(resultSet.getString("asset")).thenReturn(asset);
        when(resultSet.getBigDecimal("amount")).thenReturn(amount);
        when(resultSet.getObject("created_at", OffsetDateTime.class)).thenReturn(OffsetDateTime.now());
        return resultSet;
    }
}
