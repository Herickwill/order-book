package com.meli.orderbook.api.wallet.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

/** {@link WalletRepository} sobre o Postgres, com {@code JdbcClient}. */
@Repository
public class PostgresWalletRepository implements WalletRepository {

    private static final String FIND_BY_USER_ID = """
            SELECT user_id, asset, available, reserved
            FROM wallets
            WHERE user_id = :userId
            """;

    private static final WalletRowMapper ROW_MAPPER = new WalletRowMapper();

    private final JdbcClient jdbcClient;

    public PostgresWalletRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<WalletRow> findByUserId(String userId) {
        return jdbcClient.sql(FIND_BY_USER_ID)
                .param("userId", userId)
                .query(ROW_MAPPER)
                .list();
    }
}
