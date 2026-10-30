package com.meli.orderbook.worker.matching;

import com.meli.orderbook.shared.order.OrderSide;
import com.meli.orderbook.shared.order.OrderType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class OrderBookTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-10-29T12:00:00Z");

    @Test
    void should_order_bids_by_highest_price_then_oldest_seq_and_asks_by_lowest_price() {
        var book = new OrderBook();
        var cheapBid = order(1, OrderSide.BUY, "9.00");
        var bestBidOld = order(2, OrderSide.BUY, "10.00");
        var bestBidNew = order(3, OrderSide.BUY, "10.00");
        var highAsk = order(4, OrderSide.SELL, "12.00");
        var bestAsk = order(5, OrderSide.SELL, "11.00");
        book.add(cheapBid);
        book.add(bestBidOld);
        book.add(bestBidNew);
        book.add(highAsk);
        book.add(bestAsk);

        assertThat(book.ordersInPriority(OrderSide.BUY)).containsExactly(bestBidOld, bestBidNew, cheapBid);
        assertThat(book.ordersInPriority(OrderSide.SELL)).containsExactly(bestAsk, highAsk);
    }

    @Test
    void should_treat_prices_with_different_scales_as_the_same_level() {
        var book = new OrderBook();
        var first = order(1, OrderSide.SELL, "10.5");
        var second = order(2, OrderSide.SELL, "10.50");
        book.add(first);
        book.add(second);

        assertThat(book.bestOpposite(OrderSide.BUY)).hasValueSatisfying(
                level -> assertThat(level.orders()).containsExactly(first, second));
    }

    @Test
    void should_drop_the_price_level_when_its_last_order_is_removed() {
        var book = new OrderBook();
        var ask = order(1, OrderSide.SELL, "10.00");
        var worseAsk = order(2, OrderSide.SELL, "11.00");
        book.add(ask);
        book.add(worseAsk);

        book.remove(ask);

        assertThat(book.bestPrice(OrderSide.SELL)).hasValueSatisfying(
                price -> assertThat(price).isEqualByComparingTo("11.00"));
        assertThat(book.find(ask.id())).isEmpty();
    }

    @Test
    void should_keep_the_queue_order_when_an_order_in_the_middle_is_removed() {
        var book = new OrderBook();
        var first = order(1, OrderSide.BUY, "10.00");
        var middle = order(2, OrderSide.BUY, "10.00");
        var last = order(3, OrderSide.BUY, "10.00");
        book.add(first);
        book.add(middle);
        book.add(last);

        book.remove(middle);

        assertThat(book.ordersInPriority(OrderSide.BUY)).containsExactly(first, last);
    }

    @Test
    void should_refuse_a_market_order() {
        var book = new OrderBook();
        var market = Order.accept(UUID.randomUUID(), "user", 1, OrderSide.BUY, OrderType.MARKET, null, 1,
                CREATED_AT);

        assertThatIllegalArgumentException().isThrownBy(() -> book.add(market));
    }

    private static Order order(long seq, OrderSide side, String price) {
        return Order.accept(new UUID(0, seq), "user-" + seq, seq, side, OrderType.LIMIT, new BigDecimal(price), 5,
                CREATED_AT);
    }
}
