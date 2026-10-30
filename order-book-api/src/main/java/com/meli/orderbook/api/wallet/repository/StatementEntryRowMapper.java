package com.meli.orderbook.api.wallet.repository;

import com.meli.orderbook.shared.order.OrderSide;
import com.meli.orderbook.shared.wallet.Asset;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Converte uma linha da consulta de extrato em {@link StatementEntryRow}. */
final class StatementEntryRowMapper implements RowMapper<StatementEntryRow> {

    @Override
    public StatementEntryRow mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new StatementEntryRow(
                resultSet.getLong("reference_id"),
                resultSet.getObject("occurred_at", OffsetDateTime.class),
                enumOrNull(OrderSide.class, resultSet.getString("side")),
                resultSet.getObject("order_id", UUID.class),
                resultSet.getBigDecimal("price"),
                resultSet.getObject("quantity", Long.class),
                resultSet.getString("idempotency_key"),
                enumOrNull(Asset.class, resultSet.getString("asset")),
                resultSet.getBigDecimal("amount"));
    }

    /** As colunas de depósito vêm nulas num trade, e as de trade num depósito. */
    private static <E extends Enum<E>> E enumOrNull(Class<E> type, String value) {
        if (value == null) {
            return null;
        }

        return Enum.valueOf(type, value);
    }
}
