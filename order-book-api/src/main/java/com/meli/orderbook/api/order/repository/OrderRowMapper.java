package com.meli.orderbook.api.order.repository;

import com.meli.orderbook.shared.order.OrderSide;
import com.meli.orderbook.shared.order.OrderStatus;
import com.meli.orderbook.shared.order.OrderType;
import com.meli.orderbook.shared.order.RejectReason;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Converte uma linha de {@code orders} em {@link OrderRow}, lendo cada coluna no seu tipo. */
final class OrderRowMapper implements RowMapper<OrderRow> {

    @Override
    public OrderRow mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new OrderRow(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("user_id"),
                resultSet.getLong("seq"),
                OrderSide.valueOf(resultSet.getString("side")),
                OrderType.valueOf(resultSet.getString("type")),
                resultSet.getBigDecimal("price"),
                resultSet.getLong("quantity"),
                resultSet.getLong("filled_quantity"),
                OrderStatus.valueOf(resultSet.getString("status")),
                rejectReasonOf(resultSet),
                resultSet.getObject("created_at", OffsetDateTime.class));
    }

    /** Só ordens rejeitadas têm motivo; nas demais, a coluna é nula. */
    private static RejectReason rejectReasonOf(ResultSet resultSet) throws SQLException {
        var value = resultSet.getString("reject_reason");

        if (value == null) {
            return null;
        }

        return RejectReason.valueOf(value);
    }
}
