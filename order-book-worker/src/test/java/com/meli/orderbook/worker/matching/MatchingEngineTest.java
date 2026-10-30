package com.meli.orderbook.worker.matching;

import com.meli.orderbook.shared.order.OrderSide;
import com.meli.orderbook.shared.order.OrderStatus;
import com.meli.orderbook.shared.order.OrderType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MatchingEngineTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-10-29T12:00:00Z");

    private final OrderBook book = new OrderBook();
    private final MatchingEngine engine = new MatchingEngine(book);
    private long nextSeq = 1;

    @Test
    void should_buy_only_the_whole_units_the_budget_pays_for() {
        book.add(ask("12.00", 10));
        var buy = marketBuy(10);

        var result = engine.matchWithBudget(buy, new BigDecimal("60.00"));

        assertThat(result.trades()).singleElement().satisfies(trade -> assertThat(trade.quantity()).isEqualTo(5));
        assertThat(result.totalCost()).isEqualByComparingTo("60.00");
    }

    @Test
    void should_round_the_affordable_units_down_without_rounding_money() {
        book.add(ask("13.00", 10));
        var buy = marketBuy(10);

        var result = engine.matchWithBudget(buy, new BigDecimal("60.00"));

        assertThat(result.trades()).singleElement().satisfies(trade -> assertThat(trade.quantity()).isEqualTo(4));
        assertThat(result.totalCost()).isEqualByComparingTo("52.00");
    }

    @Test
    void should_not_trade_when_the_budget_cannot_pay_one_unit() {
        book.add(ask("10.00", 1));
        var buy = marketBuy(1);

        var result = engine.matchWithBudget(buy, new BigDecimal("9.99"));

        assertThat(result.trades()).isEmpty();
        assertThat(result.totalCost()).isEqualByComparingTo("0");
    }

    @Test
    void should_spend_the_budget_across_levels_and_stop_when_the_rest_pays_no_unit() {
        book.add(ask("10.00", 3));
        var expensive = ask("12.00", 10);
        book.add(expensive);
        var buy = marketBuy(10);

        var result = engine.matchWithBudget(buy, new BigDecimal("60.00"));

        assertThat(result.trades()).extracting(Trade::quantity).containsExactly(3L, 2L);
        assertThat(result.totalCost()).isEqualByComparingTo("54.00");
        assertThat(expensive.remaining()).isEqualTo(8);
        assertThat(buy.filledQuantity()).isEqualTo(5);
    }

    @Test
    void should_stop_at_the_limit_price_and_remove_filled_makers_from_the_book() {
        var cheap = ask("10.00", 2);
        book.add(cheap);
        book.add(ask("10.01", 5));
        var buy = limitBuy("10.00", 5);

        var result = engine.match(buy);

        assertThat(result.trades()).singleElement().satisfies(trade -> assertThat(trade.price()).isEqualByComparingTo("10.00"));
        assertThat(result.touchedMakers()).containsExactly(cheap);
        assertThat(cheap.status()).isEqualTo(OrderStatus.FILLED);
        assertThat(book.find(cheap.id())).isEmpty();
        assertThat(buy.remaining()).isEqualTo(3);
    }

    private Order ask(String price, long quantity) {
        var seq = nextSeq++;
        return Order.accept(new UUID(0, seq), "seller", seq, OrderSide.SELL, OrderType.LIMIT, new BigDecimal(price),
                quantity, CREATED_AT);
    }

    private Order limitBuy(String price, long quantity) {
        var seq = nextSeq++;
        return Order.accept(new UUID(0, seq), "buyer", seq, OrderSide.BUY, OrderType.LIMIT, new BigDecimal(price),
                quantity, CREATED_AT);
    }

    private Order marketBuy(long quantity) {
        var seq = nextSeq++;
        return Order.accept(new UUID(0, seq), "buyer", seq, OrderSide.BUY, OrderType.MARKET, null, quantity,
                CREATED_AT);
    }
}
