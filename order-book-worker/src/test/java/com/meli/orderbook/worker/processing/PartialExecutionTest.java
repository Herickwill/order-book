package com.meli.orderbook.worker.processing;

import com.meli.orderbook.shared.order.OrderSide;
import com.meli.orderbook.shared.order.OrderStatus;
import org.junit.jupiter.api.Test;

import static com.meli.orderbook.shared.wallet.Asset.BRL;
import static com.meli.orderbook.shared.wallet.Asset.VIB;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Casos de execução parcial: posição na fila, cancelamento, vários lotes,
 * restart, ordem tocada várias vezes no lote e self-trade.
 */
class PartialExecutionTest {

    @Test
    void should_rest_at_the_end_of_its_price_queue_with_the_newest_seq_when_taker_fills_partially() {
        var exchange = TestExchange.empty();
        exchange.deposit("seller", VIB, "1");
        exchange.deposit("taker", BRL, "100.00");
        exchange.deposit("late", BRL, "100.00");
        var ask = exchange.limitSell("seller", "10.00", 1);

        var taker = exchange.limitBuy("taker", "10.00", 3);
        var late = exchange.limitBuy("late", "10.00", 1);

        assertThat(exchange.order(taker).status()).isEqualTo(OrderStatus.PARTIALLY_FILLED);
        assertThat(exchange.order(taker).seq()).isGreaterThan(exchange.order(ask).seq());
        assertThat(exchange.bookOrderIds(OrderSide.BUY)).containsExactly(taker, late);
    }

    @Test
    void should_keep_maker_at_the_head_with_the_same_seq_when_it_fills_partially() {
        var exchange = TestExchange.empty();
        exchange.deposit("buyer-1", BRL, "50.00");
        exchange.deposit("buyer-2", BRL, "50.00");
        exchange.deposit("seller", VIB, "6");
        var first = exchange.limitBuy("buyer-1", "10.00", 5);
        var second = exchange.limitBuy("buyer-2", "10.00", 5);
        var seqBefore = exchange.order(first).seq();

        exchange.limitSell("seller", "10.00", 2);

        assertThat(exchange.bookOrderIds(OrderSide.BUY)).containsExactly(first, second);
        assertThat(exchange.order(first).seq()).isEqualTo(seqBefore);
        assertThat(exchange.order(first).remaining()).isEqualTo(3);

        exchange.limitSell("seller", "10.00", 4);

        assertThat(exchange.order(first).status()).isEqualTo(OrderStatus.FILLED);
        assertThat(exchange.order(second).filledQuantity()).isEqualTo(1);
        assertThat(exchange.bookOrderIds(OrderSide.BUY)).containsExactly(second);
    }

    @Test
    void should_release_only_the_remaining_reserve_and_keep_filled_quantity_when_partial_order_is_cancelled() {
        var exchange = TestExchange.empty();
        exchange.deposit("buyer", BRL, "100.00");
        exchange.deposit("seller", VIB, "4");
        var buy = exchange.limitBuy("buyer", "10.00", 10);
        exchange.limitSell("seller", "10.00", 4);

        exchange.cancel("buyer", buy);

        assertThat(exchange.order(buy).status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(exchange.order(buy).filledQuantity()).isEqualTo(4);
        assertThat(exchange.trades()).hasSize(1);
        assertThat(exchange.reserved("buyer", BRL)).isEqualByComparingTo("0.00");
        assertThat(exchange.available("buyer", BRL)).isEqualByComparingTo("60.00");
        assertThat(exchange.available("buyer", VIB)).isEqualByComparingTo("4");
        assertThat(exchange.bookOrderIds(OrderSide.BUY)).isEmpty();
    }

    @Test
    void should_leave_the_book_when_partial_fills_in_several_batches_complete_the_order() {
        var exchange = TestExchange.empty();
        exchange.deposit("seller", VIB, "6");
        exchange.deposit("buyer", BRL, "100.00");
        var sell = exchange.limitSell("seller", "10.00", 6);

        exchange.limitBuy("buyer", "10.00", 2);
        exchange.limitBuy("buyer", "10.00", 3);
        assertThat(exchange.order(sell).status()).isEqualTo(OrderStatus.PARTIALLY_FILLED);

        exchange.limitBuy("buyer", "10.00", 1);

        assertThat(exchange.order(sell).status()).isEqualTo(OrderStatus.FILLED);
        assertThat(exchange.book().find(sell)).isEmpty();
        assertThat(exchange.reserved("seller", VIB)).isEqualByComparingTo("0");
    }

    @Test
    void should_restore_partial_makers_at_the_head_and_the_rested_taker_at_the_end_when_worker_restarts() {
        var exchange = TestExchange.empty();
        exchange.deposit("buyer-1", BRL, "100.00");
        exchange.deposit("buyer-2", BRL, "100.00");
        exchange.deposit("seller", VIB, "10");
        var partialMaker = exchange.limitBuy("buyer-1", "10.00", 5);
        var waiting = exchange.limitBuy("buyer-2", "10.00", 5);
        var lastSellBeforeRestart = exchange.limitSell("seller", "10.00", 2);

        var restarted = TestExchange.restartedFrom(exchange);
        assertThat(restarted.bookOrderIds(OrderSide.BUY)).containsExactly(partialMaker, waiting);
        assertThat(restarted.book().find(partialMaker)).hasValueSatisfying(
                order -> assertThat(order.remaining()).isEqualTo(3));

        var sellAfterRestart = restarted.limitSell("seller", "10.00", 4);

        assertThat(restarted.order(partialMaker).status()).isEqualTo(OrderStatus.FILLED);
        assertThat(restarted.order(waiting).filledQuantity()).isEqualTo(1);
        assertThat(restarted.bookOrderIds(OrderSide.BUY)).containsExactly(waiting);
        assertThat(restarted.reserved("buyer-2", BRL)).isEqualByComparingTo("40.00");
        assertThat(restarted.order(sellAfterRestart).seq())
                .isEqualTo(exchange.order(lastSellBeforeRestart).seq() + 1);
    }

    @Test
    void should_record_only_the_final_state_once_when_an_order_is_touched_several_times_in_one_batch() {
        var exchange = TestExchange.empty();
        exchange.deposit("buyer", BRL, "50.00");
        exchange.deposit("seller", VIB, "5");
        var buy = exchange.limitBuy("buyer", "10.00", 5);

        var changes = exchange.batch(
                exchange.limitCommand("seller", OrderSide.SELL, "10.00", 2),
                exchange.limitCommand("seller", OrderSide.SELL, "10.00", 3));

        var touchedBuys = changes.orders().stream().filter(order -> order.id().equals(buy)).toList();
        assertThat(touchedBuys).singleElement().satisfies(order -> {
            assertThat(order.status()).isEqualTo(OrderStatus.FILLED);
            assertThat(order.filledQuantity()).isEqualTo(5);
        });
        assertThat(changes.trades()).hasSize(2);
        assertThat(changes.commandIds()).hasSize(2);
    }

    @Test
    void should_find_the_order_in_the_book_when_it_is_created_and_cancelled_in_the_same_batch() {
        var exchange = TestExchange.empty();
        exchange.deposit("buyer", BRL, "50.00");
        var create = exchange.limitCommand("buyer", OrderSide.BUY, "10.00", 5);
        var cancel = exchange.cancelCommand("buyer", create.orderId());

        var changes = exchange.batch(create, cancel);

        assertThat(exchange.order(create.orderId()).status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(changes.orders()).hasSize(1);
        assertThat(exchange.available("buyer", BRL)).isEqualByComparingTo("50.00");
        assertThat(exchange.reserved("buyer", BRL)).isEqualByComparingTo("0.00");
    }

    @Test
    void should_keep_balances_consistent_when_a_user_trades_partially_with_himself() {
        var exchange = TestExchange.empty();
        exchange.deposit("trader", BRL, "100.00");
        exchange.deposit("trader", VIB, "10");
        var sell = exchange.limitSell("trader", "10.00", 10);

        var buy = exchange.limitBuy("trader", "12.00", 4);

        assertThat(exchange.order(sell).status()).isEqualTo(OrderStatus.PARTIALLY_FILLED);
        assertThat(exchange.order(buy).status()).isEqualTo(OrderStatus.FILLED);
        assertThat(exchange.available("trader", BRL)).isEqualByComparingTo("100.00");
        assertThat(exchange.reserved("trader", BRL)).isEqualByComparingTo("0.00");
        assertThat(exchange.available("trader", VIB)).isEqualByComparingTo("4");
        assertThat(exchange.reserved("trader", VIB)).isEqualByComparingTo("6");
    }
}
