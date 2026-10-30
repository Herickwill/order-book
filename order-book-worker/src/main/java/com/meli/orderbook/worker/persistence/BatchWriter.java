package com.meli.orderbook.worker.persistence;

import com.meli.orderbook.worker.matching.Order;
import com.meli.orderbook.worker.matching.Trade;
import com.meli.orderbook.worker.processing.BatchChanges;
import com.meli.orderbook.worker.settlement.Wallet;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Types;
import java.util.List;

/**
 * Grava um lote numa única transação, nesta ordem:
 *
 * <ol>
 *   <li>confere que esta sessão ainda é a dona do advisory lock;</li>
 *   <li>upsert de {@code orders} (antes de {@code trades}, por causa da FK);</li>
 *   <li>insert de {@code trades};</li>
 *   <li>upsert de {@code wallets}, com valores absolutos;</li>
 *   <li>marca os comandos como {@code PROCESSED} e confere que mudou
 *       exatamente o tamanho do lote.</li>
 * </ol>
 *
 * <p>Marcar os comandos na mesma transação dos efeitos é o que dá
 * exactly-once: ou o lote inteiro e os {@code PROCESSED} entram, ou nada entra
 * e os comandos continuam {@code RECEIVED}.
 *
 * <p>As três escritas de linhas usam {@code NamedParameterJdbcTemplate.batchUpdate},
 * a única exceção ao {@code JdbcClient} no projeto: ele não tem escrita em lote.
 * Com {@code reWriteBatchedInserts=true}, o driver reescreve o lote em INSERTs
 * de várias linhas, um round trip por tabela.
 *
 * <p>Qualquer exceção desfaz a transação e sobe: quem chamou encerra o
 * processo, e a memória, que ficou à frente do banco, é descartada.
 */
@Component
public class BatchWriter {

    private static final String UPSERT_ORDER = """
            INSERT INTO orders
                (id, user_id, seq, side, type, price, quantity,
                 filled_quantity, status, reject_reason, created_at)
            VALUES
                (:id, :userId, :seq, :side, :type, :price, :quantity,
                 :filledQuantity, :status, :rejectReason, :createdAt)
            ON CONFLICT (id) DO UPDATE SET
                filled_quantity = EXCLUDED.filled_quantity,
                status          = EXCLUDED.status,
                reject_reason   = EXCLUDED.reject_reason
            """;

    private static final String INSERT_TRADE = """
            INSERT INTO trades
                (buy_order_id, sell_order_id, buy_user_id, sell_user_id, price, quantity)
            VALUES
                (:buyOrderId, :sellOrderId, :buyUserId, :sellUserId, :price, :quantity)
            """;

    private static final String UPSERT_WALLET = """
            INSERT INTO wallets (user_id, asset, available, reserved)
            VALUES (:userId, :asset, :available, :reserved)
            ON CONFLICT (user_id, asset) DO UPDATE SET
                available = EXCLUDED.available,
                reserved  = EXCLUDED.reserved
            """;

    /**
     * O filtro {@code status = 'RECEIVED'} faz parte da guarda de contagem: se
     * outro processo já marcou algum destes comandos, menos linhas mudam e o
     * lote é desfeito.
     */
    private static final String MARK_PROCESSED = """
            UPDATE commands
            SET status = 'PROCESSED'
            WHERE id IN (:ids)
              AND status = 'RECEIVED'
            """;

    private final JdbcClient jdbcClient;
    private final NamedParameterJdbcTemplate batchTemplate;
    private final TransactionTemplate transactionTemplate;
    private final WorkerLock workerLock;

    public BatchWriter(
            JdbcClient jdbcClient,
            NamedParameterJdbcTemplate batchTemplate,
            TransactionTemplate transactionTemplate,
            WorkerLock workerLock) {

        this.jdbcClient = jdbcClient;
        this.batchTemplate = batchTemplate;
        this.transactionTemplate = transactionTemplate;
        this.workerLock = workerLock;
    }

    /**
     * Grava o lote inteiro, ou nada.
     *
     * @param changes o estado final de tudo que o lote tocou
     * @throws LockLostException se a sessão perdeu o lock
     * @throws IllegalStateException se o {@code UPDATE} de {@code commands} não alterou
     *                               exatamente o tamanho do lote
     * @throws org.springframework.dao.DataAccessException se o banco recusar qualquer escrita
     */
    public void write(BatchChanges changes) {
        transactionTemplate.executeWithoutResult(status -> writeInTransaction(changes));
    }

    private void writeInTransaction(BatchChanges changes) {
        workerLock.requireHeld();

        upsertOrders(changes.orders());
        insertTrades(changes.trades());
        upsertWallets(changes.wallets());
        markProcessed(changes.commandIds());
    }

    private void upsertOrders(List<Order> orders) {
        if (orders.isEmpty()) {
            return;
        }

        var batch = orders.stream().map(BatchWriter::orderParameters).toArray(SqlParameterSource[]::new);
        batchTemplate.batchUpdate(UPSERT_ORDER, batch);
    }

    private void insertTrades(List<Trade> trades) {
        if (trades.isEmpty()) {
            return;
        }

        var batch = trades.stream().map(BatchWriter::tradeParameters).toArray(SqlParameterSource[]::new);
        batchTemplate.batchUpdate(INSERT_TRADE, batch);
    }

    private void upsertWallets(List<Wallet> wallets) {
        if (wallets.isEmpty()) {
            return;
        }

        var batch = wallets.stream().map(BatchWriter::walletParameters).toArray(SqlParameterSource[]::new);
        batchTemplate.batchUpdate(UPSERT_WALLET, batch);
    }

    /**
     * Segunda proteção contra dois Workers, depois do advisory lock: menos
     * linhas que o lote significa que outro processo marcou parte destes
     * comandos. Lançar a exceção desfaz a transação.
     */
    private void markProcessed(List<Long> commandIds) {
        var updatedRows = jdbcClient.sql(MARK_PROCESSED)
                .param("ids", commandIds)
                .update();

        var expectedRows = commandIds.size();
        if (updatedRows != expectedRows) {
            throw new IllegalStateException(
                    "Comandos do lote já processados por outro escritor. esperado=%d alterado=%d"
                            .formatted(expectedRows, updatedRows));
        }
    }

    private static SqlParameterSource orderParameters(Order order) {
        return new MapSqlParameterSource()
                .addValue("id", order.id(), Types.OTHER)
                .addValue("userId", order.userId())
                .addValue("seq", order.seq(), Types.BIGINT)
                .addValue("side", order.side().name(), Types.VARCHAR)
                .addValue("type", order.type().name(), Types.VARCHAR)
                .addValue("price", order.price(), Types.NUMERIC)
                .addValue("quantity", order.quantity(), Types.BIGINT)
                .addValue("filledQuantity", order.filledQuantity(), Types.BIGINT)
                .addValue("status", order.status().name(), Types.VARCHAR)
                .addValue("rejectReason", nameOf(order.rejectReason()), Types.VARCHAR)
                .addValue("createdAt", order.createdAt(), Types.TIMESTAMP_WITH_TIMEZONE);
    }

    private static SqlParameterSource tradeParameters(Trade trade) {
        return new MapSqlParameterSource()
                .addValue("buyOrderId", trade.buyOrderId(), Types.OTHER)
                .addValue("sellOrderId", trade.sellOrderId(), Types.OTHER)
                .addValue("buyUserId", trade.buyUserId())
                .addValue("sellUserId", trade.sellUserId())
                .addValue("price", trade.price(), Types.NUMERIC)
                .addValue("quantity", trade.quantity(), Types.BIGINT);
    }

    private static SqlParameterSource walletParameters(Wallet wallet) {
        return new MapSqlParameterSource()
                .addValue("userId", wallet.userId())
                .addValue("asset", wallet.asset().name(), Types.VARCHAR)
                .addValue("available", wallet.available(), Types.NUMERIC)
                .addValue("reserved", wallet.reserved(), Types.NUMERIC);
    }

    private static String nameOf(Enum<?> value) {
        if (value == null) {
            return null;
        }

        return value.name();
    }
}
