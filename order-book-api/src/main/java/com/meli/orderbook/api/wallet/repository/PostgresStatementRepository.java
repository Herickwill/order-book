package com.meli.orderbook.api.wallet.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

/** {@link StatementRepository} sobre o Postgres, com {@code JdbcClient}. */
@Repository
public class PostgresStatementRepository implements StatementRepository {

    /**
     * Três partes: depósitos processados, trades como comprador e trades como
     * vendedor.
     *
     * <p>Como em {@code PostgresTradeRepository}, cada parte tem o seu
     * {@code ORDER BY ... LIMIT :window}: o Postgres não leva o LIMIT externo
     * para dentro do UNION ALL, e sem ele leria o histórico inteiro do usuário
     * para devolver uma página.
     *
     * <p>Nos trades, ordenar por {@code id} dá a mesma ordem que por
     * {@code created_at}: o Worker é único e grava um lote depois do outro, e
     * {@code created_at} é a hora da transação do lote. Isso deixa as duas
     * partes de trades usarem os índices {@code ix_trades_*_user}. Nos
     * depósitos a ordem interna é a mesma da externa; a parte usa
     * {@code uq_commands_idempotency} para achar os comandos do usuário.
     *
     * <p>O desempate final por tipo e id deixa a ordem estável num self-trade
     * e entre linhas com o mesmo instante.
     */
    private static final String FIND_BY_USER = """
            (SELECT id AS reference_id, created_at AS occurred_at,
                    NULL AS side, NULL::uuid AS order_id, NULL::numeric AS price, NULL::bigint AS quantity,
                    idempotency_key, asset, amount
             FROM commands
             WHERE user_id = :userId
               AND type = 'DEPOSIT'
               AND status = 'PROCESSED'
             ORDER BY created_at DESC, id DESC
             LIMIT :window)
            UNION ALL
            (SELECT id, created_at,
                    'BUY', buy_order_id, price, quantity,
                    NULL, NULL, NULL
             FROM trades
             WHERE buy_user_id = :userId
             ORDER BY id DESC
             LIMIT :window)
            UNION ALL
            (SELECT id, created_at,
                    'SELL', sell_order_id, price, quantity,
                    NULL, NULL, NULL
             FROM trades
             WHERE sell_user_id = :userId
             ORDER BY id DESC
             LIMIT :window)
            ORDER BY occurred_at DESC, side NULLS FIRST, reference_id DESC
            LIMIT :limit OFFSET :offset
            """;

    private static final StatementEntryRowMapper ROW_MAPPER = new StatementEntryRowMapper();

    private final JdbcClient jdbcClient;

    public PostgresStatementRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<StatementEntryRow> findByUser(String userId, int limit, long offset) {
        var rowsNeededFromEachPart = offset + limit;
        return jdbcClient.sql(FIND_BY_USER)
                .param("userId", userId)
                .param("window", rowsNeededFromEachPart)
                .param("limit", limit)
                .param("offset", offset)
                .query(ROW_MAPPER)
                .list();
    }
}
