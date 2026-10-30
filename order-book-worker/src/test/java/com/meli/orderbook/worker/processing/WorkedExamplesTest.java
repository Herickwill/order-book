package com.meli.orderbook.worker.processing;

import com.meli.orderbook.shared.order.OrderSide;
import com.meli.orderbook.shared.order.OrderStatus;
import org.junit.jupiter.api.Test;

import static com.meli.orderbook.shared.wallet.Asset.BRL;
import static com.meli.orderbook.shared.wallet.Asset.VIB;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Três exemplos completos, conferidos centavo a centavo: uma compra
 * {@code LIMIT} que cruza dois níveis e recebe a melhora de preço, uma
 * {@code LIMIT} parcial que descansa no livro e uma compra {@code MARKET}
 * limitada pelo saldo.
 */
class WorkedExamplesTest {

    @Test
    void should_fill_across_two_levels_in_fifo_and_return_price_improvement_when_limit_buy_crosses() {
        var exchange = TestExchange.empty();
        exchange.deposit("seller-a", VIB, "5");
        exchange.deposit("seller-b", VIB, "5");
        exchange.deposit("seller-c", VIB, "10");
        exchange.deposit("buyer-d", BRL, "1212.00");
        var a = exchange.limitSell("seller-a", "100.00", 5);
        var b = exchange.limitSell("seller-b", "100.00", 5);
        var c = exchange.limitSell("seller-c", "101.00", 10);

        var d = exchange.limitBuy("buyer-d", "101.00", 12);

        var trades = exchange.trades();
        assertThat(trades).hasSize(3);
        assertThat(trades.get(0).sellOrderId()).isEqualTo(a);
        assertThat(trades.get(0).price()).isEqualByComparingTo("100.00");
        assertThat(trades.get(0).quantity()).isEqualTo(5);
        assertThat(trades.get(1).sellOrderId()).isEqualTo(b);
        assertThat(trades.get(1).price()).isEqualByComparingTo("100.00");
        assertThat(trades.get(1).quantity()).isEqualTo(5);
        assertThat(trades.get(2).sellOrderId()).isEqualTo(c);
        assertThat(trades.get(2).price()).isEqualByComparingTo("101.00");
        assertThat(trades.get(2).quantity()).isEqualTo(2);

        assertThat(exchange.order(a).status()).isEqualTo(OrderStatus.FILLED);
        assertThat(exchange.order(b).status()).isEqualTo(OrderStatus.FILLED);
        assertThat(exchange.order(c).status()).isEqualTo(OrderStatus.PARTIALLY_FILLED);
        assertThat(exchange.order(c).remaining()).isEqualTo(8);
        assertThat(exchange.order(d).status()).isEqualTo(OrderStatus.FILLED);

        assertThat(exchange.available("buyer-d", VIB)).isEqualByComparingTo("12");
        assertThat(exchange.available("buyer-d", BRL)).isEqualByComparingTo("10.00");
        assertThat(exchange.reserved("buyer-d", BRL)).isEqualByComparingTo("0.00");
        assertThat(exchange.book().bestPrice(OrderSide.SELL)).hasValueSatisfying(
                price -> assertThat(price).isEqualByComparingTo("101.00"));
    }

    @Test
    void should_rest_remaining_with_exact_reserve_when_limit_buy_fills_partially() {
        var exchange = TestExchange.empty();
        exchange.deposit("seller-a", VIB, "3");
        exchange.deposit("seller-b", VIB, "5");
        exchange.deposit("buyer-d", BRL, "200.00");
        var a = exchange.limitSell("seller-a", "10.00", 3);
        var b = exchange.limitSell("seller-b", "10.50", 5);

        var d = exchange.limitBuy("buyer-d", "10.50", 10);

        assertThat(exchange.order(a).status()).isEqualTo(OrderStatus.FILLED);
        assertThat(exchange.order(b).status()).isEqualTo(OrderStatus.FILLED);
        assertThat(exchange.order(d).status()).isEqualTo(OrderStatus.PARTIALLY_FILLED);
        assertThat(exchange.order(d).filledQuantity()).isEqualTo(8);
        assertThat(exchange.bookOrderIds(OrderSide.BUY)).containsExactly(d);

        assertThat(exchange.available("buyer-d", BRL)).isEqualByComparingTo("96.50");
        assertThat(exchange.reserved("buyer-d", BRL)).isEqualByComparingTo("21.00");
        assertThat(exchange.available("buyer-d", VIB)).isEqualByComparingTo("8");
        assertThat(exchange.available("seller-a", BRL)).isEqualByComparingTo("30.00");
        assertThat(exchange.available("seller-b", BRL)).isEqualByComparingTo("52.50");
    }

    @Test
    void should_stop_when_budget_cannot_pay_one_unit_and_return_leftover_when_market_buy_runs_out() {
        var exchange = TestExchange.empty();
        exchange.deposit("seller-a", VIB, "3");
        exchange.deposit("seller-b", VIB, "5");
        exchange.deposit("buyer-e", BRL, "60.00");
        var a = exchange.limitSell("seller-a", "10.00", 3);
        var b = exchange.limitSell("seller-b", "12.00", 5);

        var e = exchange.marketBuy("buyer-e", 10);

        var trades = exchange.trades();
        assertThat(trades).hasSize(2);
        assertThat(trades.get(0).quantity()).isEqualTo(3);
        assertThat(trades.get(0).price()).isEqualByComparingTo("10.00");
        assertThat(trades.get(1).quantity()).isEqualTo(2);
        assertThat(trades.get(1).price()).isEqualByComparingTo("12.00");

        assertThat(exchange.order(e).status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(exchange.order(e).filledQuantity()).isEqualTo(5);
        assertThat(exchange.order(a).status()).isEqualTo(OrderStatus.FILLED);
        assertThat(exchange.order(b).status()).isEqualTo(OrderStatus.PARTIALLY_FILLED);
        assertThat(exchange.order(b).remaining()).isEqualTo(3);
        assertThat(exchange.bookOrderIds(OrderSide.BUY)).isEmpty();

        assertThat(exchange.available("buyer-e", BRL)).isEqualByComparingTo("6.00");
        assertThat(exchange.reserved("buyer-e", BRL)).isEqualByComparingTo("0.00");
        assertThat(exchange.available("buyer-e", VIB)).isEqualByComparingTo("5");
    }
}
