package com.meli.orderbook.api.trade.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

/** {@link TradeRepository} sobre o Postgres, com {@code JdbcClient}. */
@Repository
public class PostgresTradeRepository implements TradeRepository {

    /**
     * Uma metade para os trades em que o usuário comprou e outra para os que
     * vendeu, cada uma pelo seu índice.
     *
     * <p>O ORDER BY + LIMIT dentro de cada metade é o que mantém a consulta
     * barata: o Postgres não leva o LIMIT externo para dentro do UNION ALL, e
     * sem ele leria todos os trades do usuário para devolver uma página. Com
     * ele, cada metade lê no máximo {@code offset + limit} linhas, já
     * ordenadas, e o resultado é só intercalado.
     *
     * <p>O desempate por {@code side} deixa a ordem estável num self-trade, em
     * que o mesmo id aparece nas duas metades.
     */
    private static final String FIND_BY_USER = """
            (SELECT id, buy_order_id AS order_id, 'BUY' AS side, price, quantity, created_at
             FROM trades
             WHERE buy_user_id = :userId
             ORDER BY id DESC
             LIMIT :window)
            UNION ALL
            (SELECT id, sell_order_id AS order_id, 'SELL' AS side, price, quantity, created_at
             FROM trades
             WHERE sell_user_id = :userId
             ORDER BY id DESC
             LIMIT :window)
            ORDER BY id DESC, side
            LIMIT :limit OFFSET :offset
            """;

    private static final UserTradeRowMapper ROW_MAPPER = new UserTradeRowMapper();

    private final JdbcClient jdbcClient;

    public PostgresTradeRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<UserTradeRow> findByUser(String userId, int limit, long offset) {
        var rowsNeededFromEachHalf = offset + limit;

        return jdbcClient.sql(FIND_BY_USER)
                .param("userId", userId)
                .param("window", rowsNeededFromEachHalf)
                .param("limit", limit)
                .param("offset", offset)
                .query(ROW_MAPPER)
                .list();
    }
}
