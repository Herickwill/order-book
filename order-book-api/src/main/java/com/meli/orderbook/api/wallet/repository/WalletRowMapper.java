package com.meli.orderbook.api.wallet.repository;

import com.meli.orderbook.shared.wallet.Asset;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/** Converte uma linha de {@code wallets} em {@link WalletRow}. */
final class WalletRowMapper implements RowMapper<WalletRow> {

    @Override
    public WalletRow mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new WalletRow(
                resultSet.getString("user_id"),
                Asset.valueOf(resultSet.getString("asset")),
                resultSet.getBigDecimal("available"),
                resultSet.getBigDecimal("reserved"));
    }
}
