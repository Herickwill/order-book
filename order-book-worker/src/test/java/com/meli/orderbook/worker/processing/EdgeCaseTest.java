package com.meli.orderbook.worker.processing;

import com.meli.orderbook.shared.order.OrderSide;
import com.meli.orderbook.shared.order.OrderStatus;
import com.meli.orderbook.shared.order.RejectReason;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.meli.orderbook.shared.wallet.Asset.BRL;
import static com.meli.orderbook.shared.wallet.Asset.VIB;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Casos de borda: preço do trade, limite exato, rejeições de ordem
 * {@code MARKET}, cancelamentos ignorados, depósito no limite da coluna e
 * carteiras criadas sob demanda.
 */
class EdgeCaseTest {

    @Test
    void should_trade_at_the_ask_price_when_a_buy_crosses_a_cheaper_ask() {
        var exchange = TestExchange.empty();
        exchange.deposit("seller", VIB, "1");
        exchange.deposit("buyer", BRL, "20.00");
        exchange.limitSell("seller", "9.00", 1);

        exchange.limitBuy("buyer", "10.00", 1);

        assertThat(exchange.trades()).singleElement()
                .satisfies(trade -> assertThat(trade.price()).isEqualByComparingTo("9.00"));
        assertThat(exchange.available("buyer", BRL)).isEqualByComparingTo("11.00");
    }

    @Test
    void should_trade_at_the_bid_price_when_a_sell_crosses_a_higher_bid() {
        var exchange = TestExchange.empty();
        exchange.deposit("buyer", BRL, "10.00");
        exchange.deposit("seller", VIB, "1");
        exchange.limitBuy("buyer", "10.00", 1);

        exchange.limitSell("seller", "9.00", 1);

        assertThat(exchange.trades()).singleElement()
                .satisfies(trade -> assertThat(trade.price()).isEqualByComparingTo("10.00"));
        assertThat(exchange.available("seller", BRL)).isEqualByComparingTo("10.00");
    }

    @Test
    void should_execute_when_the_maker_price_equals_the_limit() {
        var exchange = TestExchange.empty();
        exchange.deposit("seller", VIB, "1");
        exchange.deposit("buyer", BRL, "10.50");
        exchange.limitSell("seller", "10.50", 1);

        var buy = exchange.limitBuy("buyer", "10.5", 1);

        assertThat(exchange.order(buy).status()).isEqualTo(OrderStatus.FILLED);
        assertThat(exchange.available("buyer", BRL)).isEqualByComparingTo("0.00");
    }

    // Um centavo além do limite não executa
    @Test
    void should_rest_without_trading_when_the_best_ask_is_one_cent_above_the_limit() {
        var exchange = TestExchange.empty();
        exchange.deposit("seller", VIB, "1");
        exchange.deposit("buyer", BRL, "10.50");
        exchange.limitSell("seller", "10.51", 1);

        var buy = exchange.limitBuy("buyer", "10.50", 1);

        assertThat(exchange.trades()).isEmpty();
        assertThat(exchange.order(buy).status()).isEqualTo(OrderStatus.NEW);
        assertThat(exchange.bookOrderIds(OrderSide.BUY)).containsExactly(buy);
    }

    @Test
    void should_reject_with_no_liquidity_and_release_everything_when_market_buy_finds_an_empty_book() {
        var exchange = TestExchange.empty();
        exchange.deposit("buyer", BRL, "100.00");

        var buy = exchange.marketBuy("buyer", 5);

        assertThat(exchange.order(buy).status()).isEqualTo(OrderStatus.REJECTED);
        assertThat(exchange.order(buy).rejectReason()).isEqualTo(RejectReason.NO_LIQUIDITY);
        assertThat(exchange.available("buyer", BRL)).isEqualByComparingTo("100.00");
        assertThat(exchange.reserved("buyer", BRL)).isEqualByComparingTo("0.00");
    }

    // O saldo é conferido antes da liquidez
    @Test
    void should_reject_with_insufficient_funds_when_market_buy_has_no_brl_even_with_an_empty_book() {
        var exchange = TestExchange.empty();

        var buy = exchange.marketBuy("buyer", 5);

        assertThat(exchange.order(buy).rejectReason()).isEqualTo(RejectReason.INSUFFICIENT_FUNDS);
    }

    @Test
    void should_reject_with_insufficient_funds_when_market_buy_cannot_pay_one_unit_at_the_best_price() {
        var exchange = TestExchange.empty();
        exchange.deposit("seller", VIB, "1");
        exchange.deposit("buyer", BRL, "9.99");
        exchange.limitSell("seller", "10.00", 1);

        var buy = exchange.marketBuy("buyer", 1);

        assertThat(exchange.order(buy).status()).isEqualTo(OrderStatus.REJECTED);
        assertThat(exchange.order(buy).rejectReason()).isEqualTo(RejectReason.INSUFFICIENT_FUNDS);
        assertThat(exchange.available("buyer", BRL)).isEqualByComparingTo("9.99");
        assertThat(exchange.reserved("buyer", BRL)).isEqualByComparingTo("0.00");
    }

    @Test
    void should_reject_the_whole_order_and_sell_nothing_when_market_sell_lacks_vib() {
        var exchange = TestExchange.empty();
        exchange.deposit("buyer", BRL, "100.00");
        exchange.deposit("seller", VIB, "3");
        exchange.limitBuy("buyer", "10.00", 5);

        var sell = exchange.marketSell("seller", 5);

        assertThat(exchange.order(sell).status()).isEqualTo(OrderStatus.REJECTED);
        assertThat(exchange.order(sell).rejectReason()).isEqualTo(RejectReason.INSUFFICIENT_FUNDS);
        assertThat(exchange.trades()).isEmpty();
        assertThat(exchange.available("seller", VIB)).isEqualByComparingTo("3");
    }

    @Test
    void should_cancel_the_rest_and_release_remaining_vib_when_market_sell_exhausts_the_bids() {
        var exchange = TestExchange.empty();
        exchange.deposit("buyer", BRL, "100.00");
        exchange.deposit("seller", VIB, "5");
        exchange.limitBuy("buyer", "10.00", 2);

        var sell = exchange.marketSell("seller", 5);

        assertThat(exchange.order(sell).status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(exchange.order(sell).filledQuantity()).isEqualTo(2);
        assertThat(exchange.available("seller", VIB)).isEqualByComparingTo("3");
        assertThat(exchange.reserved("seller", VIB)).isEqualByComparingTo("0");
        assertThat(exchange.bookOrderIds(OrderSide.SELL)).isEmpty();
    }

    // Uma MARKET BUY que executa tudo ainda devolve a sobra
    @Test
    void should_release_unspent_brl_when_market_buy_fills_completely() {
        var exchange = TestExchange.empty();
        exchange.deposit("seller", VIB, "2");
        exchange.deposit("buyer", BRL, "100.00");
        exchange.limitSell("seller", "10.00", 2);

        var buy = exchange.marketBuy("buyer", 2);

        assertThat(exchange.order(buy).status()).isEqualTo(OrderStatus.FILLED);
        assertThat(exchange.available("buyer", BRL)).isEqualByComparingTo("80.00");
        assertThat(exchange.reserved("buyer", BRL)).isEqualByComparingTo("0.00");
    }

    @Test
    void should_change_nothing_when_cancelling_an_order_already_filled() {
        var exchange = TestExchange.empty();
        exchange.deposit("seller", VIB, "1");
        exchange.deposit("buyer", BRL, "10.00");
        var sell = exchange.limitSell("seller", "10.00", 1);
        exchange.limitBuy("buyer", "10.00", 1);

        exchange.cancel("seller", sell);

        assertThat(exchange.order(sell).status()).isEqualTo(OrderStatus.FILLED);
        assertThat(exchange.lastChanges().orders()).isEmpty();
        assertThat(exchange.lastChanges().wallets()).isEmpty();
        assertThat(exchange.lastChanges().commandIds()).hasSize(1);
    }

    @Test
    void should_change_nothing_when_cancelling_an_order_already_cancelled() {
        var exchange = TestExchange.empty();
        exchange.deposit("buyer", BRL, "10.00");
        var buy = exchange.limitBuy("buyer", "10.00", 1);
        exchange.cancel("buyer", buy);

        exchange.cancel("buyer", buy);

        assertThat(exchange.lastChanges().orders()).isEmpty();
        assertThat(exchange.available("buyer", BRL)).isEqualByComparingTo("10.00");
    }

    @Test
    void should_keep_the_order_in_the_book_when_another_user_tries_to_cancel_it() {
        var exchange = TestExchange.empty();
        exchange.deposit("owner", BRL, "10.00");
        var buy = exchange.limitBuy("owner", "10.00", 1);

        exchange.cancel("intruder", buy);

        assertThat(exchange.order(buy).status()).isEqualTo(OrderStatus.NEW);
        assertThat(exchange.bookOrderIds(OrderSide.BUY)).containsExactly(buy);
        assertThat(exchange.reserved("owner", BRL)).isEqualByComparingTo("10.00");
    }

    @Test
    void should_change_nothing_when_cancelling_an_order_that_never_existed() {
        var exchange = TestExchange.empty();

        var changes = exchange.batch(exchange.cancelCommand("anyone", UUID.randomUUID()));

        assertThat(changes.orders()).isEmpty();
        assertThat(changes.wallets()).isEmpty();
        assertThat(changes.commandIds()).hasSize(1);
    }

    @Test
    void should_change_nothing_when_cancelling_a_market_order() {
        var exchange = TestExchange.empty();
        exchange.deposit("buyer", BRL, "10.00");
        var market = exchange.marketBuy("buyer", 1);

        exchange.cancel("buyer", market);

        assertThat(exchange.order(market).status()).isEqualTo(OrderStatus.REJECTED);
        assertThat(exchange.lastChanges().orders()).isEmpty();
    }

    @Test
    void should_ignore_the_deposit_and_keep_processing_the_batch_when_it_would_overflow_the_column() {
        var exchange = TestExchange.empty();
        exchange.deposit("whale", BRL, "999999999999999.00");

        var changes = exchange.batch(
                exchange.depositCommand("whale", BRL, "1.00"),
                exchange.depositCommand("other", BRL, "5.00"));

        assertThat(exchange.available("whale", BRL)).isEqualByComparingTo("999999999999999.00");
        assertThat(exchange.available("other", BRL)).isEqualByComparingTo("5.00");
        assertThat(changes.commandIds()).hasSize(2);
        assertThat(changes.wallets()).singleElement()
                .satisfies(wallet -> assertThat(wallet.userId()).isEqualTo("other"));
    }

    // O limite exato ainda cabe
    @Test
    void should_accept_the_deposit_when_the_total_reaches_exactly_the_column_limit() {
        var exchange = TestExchange.empty();
        exchange.deposit("whale", BRL, "999999999999999.00");

        exchange.deposit("whale", BRL, "0.99");

        assertThat(exchange.available("whale", BRL)).isEqualByComparingTo("999999999999999.99");
    }

    @Test
    void should_reject_and_create_no_wallet_when_a_user_without_wallet_sends_an_order() {
        var exchange = TestExchange.empty();

        var buy = exchange.limitBuy("nobody", "10.00", 1);
        var sell = exchange.limitSell("nobody", "10.00", 1);

        assertThat(exchange.order(buy).rejectReason()).isEqualTo(RejectReason.INSUFFICIENT_FUNDS);
        assertThat(exchange.order(sell).rejectReason()).isEqualTo(RejectReason.INSUFFICIENT_FUNDS);
        assertThat(exchange.order(sell).seq()).isEqualTo(exchange.order(buy).seq() + 1);
        assertThat(exchange.hasWallet("nobody", BRL)).isFalse();
        assertThat(exchange.hasWallet("nobody", VIB)).isFalse();
        assertThat(exchange.lastChanges().wallets()).isEmpty();
    }

    @Test
    void should_create_the_brl_wallet_when_a_seller_without_one_receives_the_proceeds() {
        var exchange = TestExchange.empty();
        exchange.deposit("seller", VIB, "1");
        exchange.deposit("buyer", BRL, "10.00");
        exchange.limitBuy("buyer", "10.00", 1);
        assertThat(exchange.hasWallet("seller", BRL)).isFalse();

        exchange.limitSell("seller", "10.00", 1);

        assertThat(exchange.available("seller", BRL)).isEqualByComparingTo("10.00");
        assertThat(exchange.lastChanges().wallets())
                .anySatisfy(wallet -> {
                    assertThat(wallet.userId()).isEqualTo("seller");
                    assertThat(wallet.asset()).isEqualTo(BRL);
                });
    }
}
