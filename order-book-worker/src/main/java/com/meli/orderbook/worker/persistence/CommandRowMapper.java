package com.meli.orderbook.worker.persistence;

import com.meli.orderbook.shared.command.CommandType;
import com.meli.orderbook.shared.order.OrderSide;
import com.meli.orderbook.shared.order.OrderType;
import com.meli.orderbook.shared.wallet.Asset;
import com.meli.orderbook.worker.processing.Command;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Uma linha de {@code commands} para {@link Command}. */
class CommandRowMapper implements RowMapper<Command> {

    @Override
    public Command mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Command(
                rs.getLong("id"),
                rs.getString("user_id"),
                CommandType.valueOf(rs.getString("type")),
                rs.getObject("order_id", UUID.class),
                enumOrNull(OrderSide.class, rs.getString("side")),
                enumOrNull(OrderType.class, rs.getString("order_type")),
                rs.getObject("price", BigDecimal.class),
                rs.getObject("quantity", Long.class),
                enumOrNull(Asset.class, rs.getString("asset")),
                rs.getObject("amount", BigDecimal.class),
                rs.getObject("created_at", OffsetDateTime.class));
    }

    /** Colunas de outro tipo de comando vêm nulas. */
    private static <E extends Enum<E>> E enumOrNull(Class<E> type, String value) {
        if (value == null) {
            return null;
        }

        return Enum.valueOf(type, value);
    }
}
