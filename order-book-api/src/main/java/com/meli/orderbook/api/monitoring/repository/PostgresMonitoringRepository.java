package com.meli.orderbook.api.monitoring.repository;

import com.meli.orderbook.shared.order.OrderSide;
import com.meli.orderbook.shared.worker.WorkerLockKey;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Types;

/** {@link MonitoringRepository} sobre o Postgres, com {@code JdbcClient}. */
@Repository
public class PostgresMonitoringRepository implements MonitoringRepository {

    /** Usa o índice parcial {@code ix_commands_pending}: o custo acompanha a fila, não a tabela. */
    private static final String COUNT_PENDING_COMMANDS = """
            SELECT COUNT(*)
            FROM commands
            WHERE status = 'RECEIVED'
            """;

    private static final String OLDEST_PENDING_SECONDS = """
            SELECT COALESCE(EXTRACT(EPOCH FROM NOW() - MIN(created_at)), 0)
            FROM commands
            WHERE status = 'RECEIVED'
            """;

    /** Usa o índice parcial {@code ix_orders_open_book}. */
    private static final String COUNT_OPEN_ORDERS = """
            SELECT COUNT(*)
            FROM orders
            WHERE side = :side
              AND status IN ('NEW', 'PARTIALLY_FILLED')
            """;

    private static final String OPEN_QUANTITY = """
            SELECT COALESCE(SUM(quantity - filled_quantity), 0)
            FROM orders
            WHERE side = :side
              AND status IN ('NEW', 'PARTIALLY_FILLED')
            """;

    private static final String LAST_TRADE_ID = """
            SELECT COALESCE(MAX(id), 0)
            FROM trades
            """;

    /** Sem {@code pg_backend_pid()}: aqui interessa se <b>algum</b> Worker tem o lock. */
    private static final String WORKER_LOCK_HELD = """
            SELECT EXISTS (
                SELECT 1
                FROM pg_locks
                WHERE locktype = 'advisory'
                  AND classid = 0
                  AND objid = :key
                  AND objsubid = 1
                  AND granted)
            """;

    private final JdbcClient jdbcClient;

    public PostgresMonitoringRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public long countPendingCommands() {
        return jdbcClient.sql(COUNT_PENDING_COMMANDS)
                .query(Long.class)
                .single();
    }

    @Override
    public double oldestPendingSeconds() {
        return jdbcClient.sql(OLDEST_PENDING_SECONDS)
                .query(Double.class)
                .single();
    }

    @Override
    public long countOpenOrders(OrderSide side) {
        return jdbcClient.sql(COUNT_OPEN_ORDERS)
                .param("side", side.name(), Types.VARCHAR)
                .query(Long.class)
                .single();
    }

    @Override
    public long openQuantity(OrderSide side) {
        return jdbcClient.sql(OPEN_QUANTITY)
                .param("side", side.name(), Types.VARCHAR)
                .query(Long.class)
                .single();
    }

    @Override
    public long lastTradeId() {
        return jdbcClient.sql(LAST_TRADE_ID)
                .query(Long.class)
                .single();
    }

    @Override
    public boolean isWorkerLockHeld() {
        return jdbcClient.sql(WORKER_LOCK_HELD)
                .param("key", WorkerLockKey.ADVISORY_LOCK_KEY, Types.BIGINT)
                .query(Boolean.class)
                .single();
    }
}
