package com.meli.orderbook.api.order.repository;

import com.meli.orderbook.shared.order.OrderStatus;
import com.meli.orderbook.shared.order.RejectReason;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderRowMapperTest {

    private final OrderRowMapper mapper = new OrderRowMapper();

    @Test
    void should_leave_reject_reason_null_when_the_order_is_open() throws SQLException {
        var order = mapper.mapRow(row("NEW", null), 0);

        assertThat(order.status()).isEqualTo(OrderStatus.NEW);
        assertThat(order.rejectReason()).isNull();
    }

    @Test
    void should_map_the_reject_reason_when_the_order_was_rejected() throws SQLException {
        var order = mapper.mapRow(row("REJECTED", "NO_LIQUIDITY"), 0);

        assertThat(order.status()).isEqualTo(OrderStatus.REJECTED);
        assertThat(order.rejectReason()).isEqualTo(RejectReason.NO_LIQUIDITY);
    }

    private static ResultSet row(String status, String rejectReason) throws SQLException {
        var resultSet = mock(ResultSet.class);
        when(resultSet.getObject("id", UUID.class)).thenReturn(UUID.randomUUID());
        when(resultSet.getString("user_id")).thenReturn("alice");
        when(resultSet.getLong("seq")).thenReturn(1L);
        when(resultSet.getString("side")).thenReturn("BUY");
        when(resultSet.getString("type")).thenReturn("LIMIT");
        when(resultSet.getBigDecimal("price")).thenReturn(BigDecimal.TEN);
        when(resultSet.getLong("quantity")).thenReturn(5L);
        when(resultSet.getLong("filled_quantity")).thenReturn(0L);
        when(resultSet.getString("status")).thenReturn(status);
        when(resultSet.getString("reject_reason")).thenReturn(rejectReason);
        when(resultSet.getObject("created_at", OffsetDateTime.class)).thenReturn(OffsetDateTime.now());
        return resultSet;
    }
}
