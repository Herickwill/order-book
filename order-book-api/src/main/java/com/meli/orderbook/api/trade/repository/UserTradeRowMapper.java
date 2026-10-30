package com.meli.orderbook.api.trade.repository;

import com.meli.orderbook.shared.order.OrderSide;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Converte uma linha da consulta de histórico em {@link UserTradeRow}. */
final class UserTradeRowMapper implements RowMapper<UserTradeRow> {

    @Override
    public UserTradeRow mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new UserTradeRow(
                resultSet.getLong("id"),
                resultSet.getObject("order_id", UUID.class),
                OrderSide.valueOf(resultSet.getString("side")),
                resultSet.getBigDecimal("price"),
                resultSet.getLong("quantity"),
                resultSet.getObject("created_at", OffsetDateTime.class));
    }
}
