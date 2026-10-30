package com.meli.orderbook.worker.persistence;

import com.meli.orderbook.shared.order.OrderSide;
import com.meli.orderbook.shared.order.OrderType;
import com.meli.orderbook.shared.wallet.Asset;
import com.meli.orderbook.worker.matching.Order;
import com.meli.orderbook.worker.matching.Trade;
import com.meli.orderbook.worker.processing.BatchChanges;
import com.meli.orderbook.worker.settlement.Wallet;
import com.meli.orderbook.worker.settlement.WalletKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A ordem das escritas e as guardas da transação do lote, com o banco
 * simulado. O SQL em si não é executado aqui; ele é conferido subindo o
 * projeto e rodando {@code db/dev/check-invariants.sql}.
 */
class BatchWriterTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-10-29T12:00:00Z");

    private JdbcClient jdbcClient;
    private NamedParameterJdbcTemplate batchTemplate;
    private TransactionTemplate transactionTemplate;
    private WorkerLock workerLock;
    private BatchWriter batchWriter;

    @BeforeEach
    void setUp() {
        jdbcClient = mock(JdbcClient.class, RETURNS_DEEP_STUBS);
        batchTemplate = mock(NamedParameterJdbcTemplate.class);
        transactionTemplate = mock(TransactionTemplate.class);
        workerLock = mock(WorkerLock.class);
        batchWriter = new BatchWriter(jdbcClient, batchTemplate, transactionTemplate, workerLock);

        doAnswer(invocation -> {
            Consumer<TransactionStatus> work = invocation.getArgument(0);
            work.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    @Test
    void should_check_the_lock_first_and_write_orders_before_trades_before_wallets_before_commands() {
        markProcessedReturns(2);

        batchWriter.write(fullBatch());

        InOrder order = inOrder(workerLock, batchTemplate, jdbcClient);
        order.verify(workerLock).requireHeld();
        order.verify(batchTemplate).batchUpdate(sqlContaining("INSERT INTO orders"), any(SqlParameterSource[].class));
        order.verify(batchTemplate).batchUpdate(sqlContaining("INSERT INTO trades"), any(SqlParameterSource[].class));
        order.verify(batchTemplate).batchUpdate(sqlContaining("INSERT INTO wallets"), any(SqlParameterSource[].class));
        order.verify(jdbcClient).sql(sqlContaining("UPDATE commands"));
    }

    @Test
    void should_send_one_row_per_touched_order_in_a_single_batch() {
        markProcessedReturns(2);

        batchWriter.write(fullBatch());

        var rows = ArgumentCaptor.forClass(SqlParameterSource[].class);
        verify(batchTemplate).batchUpdate(sqlContaining("INSERT INTO orders"), rows.capture());
        assertThat(rows.getValue()).hasSize(2);
        assertThat(rows.getValue()[0].getValue("status")).isEqualTo("PARTIALLY_FILLED");
    }

    @Test
    void should_fail_inside_the_transaction_when_fewer_commands_than_the_batch_were_marked() {
        markProcessedReturns(1);

        assertThatIllegalStateException()
                .isThrownBy(() -> batchWriter.write(fullBatch()))
                .withMessageContaining("esperado=2");
    }

    @Test
    void should_write_nothing_when_the_session_lost_the_lock() {
        doThrow(new LockLostException("sem lock")).when(workerLock).requireHeld();

        assertThatThrownBy(() -> batchWriter.write(fullBatch())).isInstanceOf(LockLostException.class);

        verify(batchTemplate, never()).batchUpdate(anyString(), any(SqlParameterSource[].class));
    }

    @Test
    void should_only_mark_the_commands_when_the_batch_touched_no_order_trade_or_wallet() {
        markProcessedReturns(1);
        var ignoredCancel = new BatchChanges();
        ignoredCancel.recordProcessed(7L);

        batchWriter.write(ignoredCancel);

        verify(batchTemplate, never()).batchUpdate(anyString(), any(SqlParameterSource[].class));
        verify(jdbcClient).sql(sqlContaining("UPDATE commands"));
    }

    private void markProcessedReturns(int updatedRows) {
        when(jdbcClient.sql(anyString()).param(anyString(), any()).update()).thenReturn(updatedRows);
    }

    private static String sqlContaining(String fragment) {
        return argThat(sql -> sql != null && sql.contains(fragment));
    }

    /** Uma compra que executou contra uma venda: duas ordens, um trade, carteiras e dois comandos. */
    private static BatchChanges fullBatch() {
        var buy = Order.accept(UUID.randomUUID(), "buyer", 2, OrderSide.BUY, OrderType.LIMIT,
                new BigDecimal("10.00"), 5, CREATED_AT);
        var sell = Order.accept(UUID.randomUUID(), "seller", 1, OrderSide.SELL, OrderType.LIMIT,
                new BigDecimal("10.00"), 2, CREATED_AT);
        buy.fill(2);
        sell.fill(2);
        var trade = Trade.between(buy, sell, 2);
        var wallet = Wallet.restore(new WalletKey("buyer", Asset.BRL), new BigDecimal("70.00"), new BigDecimal("30.00"));

        var changes = new BatchChanges();
        changes.recordOrder(buy);
        changes.recordOrder(sell);
        changes.recordTrade(trade);
        changes.recordWallets(List.of(wallet));
        changes.recordProcessed(1L);
        changes.recordProcessed(2L);
        return changes;
    }
}
