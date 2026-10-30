package com.meli.orderbook.api.command.repository;

import com.meli.orderbook.shared.command.CommandStatus;
import com.meli.orderbook.shared.command.CommandType;
import com.meli.orderbook.shared.order.OrderSide;
import com.meli.orderbook.shared.order.OrderType;
import com.meli.orderbook.shared.wallet.Asset;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Converte uma linha de {@code commands} em {@link CommandRow}, lendo cada coluna no seu tipo. */
final class CommandRowMapper implements RowMapper<CommandRow> {

    @Override
    public CommandRow mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new CommandRow(
                resultSet.getLong("id"),
                resultSet.getString("user_id"),
                resultSet.getString("idempotency_key"),
                CommandType.valueOf(resultSet.getString("type")),
                CommandStatus.valueOf(resultSet.getString("status")),
                resultSet.getObject("order_id", UUID.class),
                nullableEnum(resultSet, "side", OrderSide.class),
                nullableEnum(resultSet, "order_type", OrderType.class),
                resultSet.getBigDecimal("price"),
                resultSet.getObject("quantity", Long.class),
                nullableEnum(resultSet, "asset", Asset.class),
                resultSet.getBigDecimal("amount"),
                resultSet.getObject("created_at", OffsetDateTime.class));
    }

    /** As colunas de enum específicas de um tipo de comando são nulas nos demais. */
    private static <E extends Enum<E>> E nullableEnum(ResultSet resultSet, String column, Class<E> enumType)
            throws SQLException {

        var value = resultSet.getString(column);

        if (value == null) {
            return null;
        }

        return Enum.valueOf(enumType, value);
    }
}
