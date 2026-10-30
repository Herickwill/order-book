package com.meli.orderbook.worker.persistence;

import com.meli.orderbook.shared.order.OrderSide;
import com.meli.orderbook.shared.order.OrderStatus;
import com.meli.orderbook.shared.order.OrderType;
import com.meli.orderbook.worker.matching.Order;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Uma ordem aberta de {@code orders} para {@link Order}, pronta para voltar ao livro. */
class OpenOrderRowMapper implements RowMapper<Order> {

    @Override
    public Order mapRow(ResultSet rs, int rowNum) throws SQLException {
        return Order.restore(
                rs.getObject("id", UUID.class),
                rs.getString("user_id"),
                rs.getLong("seq"),
                OrderSide.valueOf(rs.getString("side")),
                OrderType.valueOf(rs.getString("type")),
                rs.getObject("price", BigDecimal.class),
                rs.getLong("quantity"),
                rs.getLong("filled_quantity"),
                OrderStatus.valueOf(rs.getString("status")),
                rs.getObject("created_at", OffsetDateTime.class));
    }
}
