package com.meli.orderbook.worker.persistence;

import com.meli.orderbook.shared.wallet.Asset;
import com.meli.orderbook.worker.settlement.Wallet;
import com.meli.orderbook.worker.settlement.WalletKey;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Uma linha de {@code wallets} para {@link Wallet}. */
class WalletRowMapper implements RowMapper<Wallet> {

    @Override
    public Wallet mapRow(ResultSet rs, int rowNum) throws SQLException {
        var key = new WalletKey(rs.getString("user_id"), Asset.valueOf(rs.getString("asset")));
        return Wallet.restore(
                key,
                rs.getObject("available", BigDecimal.class),
                rs.getObject("reserved", BigDecimal.class));
    }
}
