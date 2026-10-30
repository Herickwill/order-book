package com.meli.orderbook.worker.processing;

import com.meli.orderbook.shared.command.CommandType;
import com.meli.orderbook.shared.order.OrderSide;
import com.meli.orderbook.shared.order.OrderType;
import com.meli.orderbook.shared.wallet.Asset;
import com.meli.orderbook.worker.matching.Order;
import com.meli.orderbook.worker.matching.OrderBook;
import com.meli.orderbook.worker.matching.Trade;
import com.meli.orderbook.worker.settlement.Wallet;
import com.meli.orderbook.worker.settlement.Wallets;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * O núcleo do Worker montado em memória, com atalhos para enviar comandos e
 * consultar o resultado. Cada atalho processa um lote de um comando; para
 * vários comandos no mesmo lote, monte-os com os métodos {@code *Command} e
 * use {@link #batch}.
 */
final class TestExchange {

    static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-10-29T12:00:00Z");

    private final OrderBook book;
    private final Wallets wallets;
    private final CommandProcessor processor;
    private final Map<UUID, Order> ordersSeen = new LinkedHashMap<>();
    private final List<Trade> trades = new ArrayList<>();
    private long nextCommandId = 1;
    private long nextOrderNumber = 1;
    private BatchChanges lastChanges;

    private TestExchange(OrderBook book, Wallets wallets, long lastSeq) {
        this.book = book;
        this.wallets = wallets;
        this.processor = new CommandProcessor(book, wallets, lastSeq);
    }

    static TestExchange empty() {
        return new TestExchange(new OrderBook(), new Wallets(), 0);
    }

    /**
     * Simula o restart: um Worker novo, reconstruído só a partir do que estaria
     * gravado (carteiras e ordens abertas, lidas em ordem de seq), como faz o
     * {@code StateLoader}. Os objetos são cópias; nada é compartilhado com o antigo.
     */
    static TestExchange restartedFrom(TestExchange previous) {
        var restoredWallets = new Wallets();
        for (var wallet : previous.wallets.all()) {
            var copy = Wallet.restore(wallet.key(), wallet.available(), wallet.reserved());
            restoredWallets.restore(copy);
        }

        var openOrders = new ArrayList<Order>();
        openOrders.addAll(previous.book.ordersInPriority(OrderSide.BUY));
        openOrders.addAll(previous.book.ordersInPriority(OrderSide.SELL));
        openOrders.sort(Comparator.comparingLong(Order::seq));

        var restoredBook = new OrderBook();
        for (var order : openOrders) {
            var copy = Order.restore(order.id(), order.userId(), order.seq(), order.side(), order.type(),
                    order.price(), order.quantity(), order.filledQuantity(), order.status(), order.createdAt());
            restoredBook.add(copy);
        }

        var lastSeq = previous.ordersSeen.values().stream().mapToLong(Order::seq).max().orElse(0);
        var restarted = new TestExchange(restoredBook, restoredWallets, lastSeq);
        restarted.nextCommandId = previous.nextCommandId;
        restarted.nextOrderNumber = previous.nextOrderNumber;
        return restarted;
    }

    // ---- comandos em lote de um ----

    void deposit(String userId, Asset asset, String amount) {
        batch(depositCommand(userId, asset, amount));
    }

    UUID limitBuy(String userId, String price, long quantity) {
        return submit(limitCommand(userId, OrderSide.BUY, price, quantity));
    }

    UUID limitSell(String userId, String price, long quantity) {
        return submit(limitCommand(userId, OrderSide.SELL, price, quantity));
    }

    UUID marketBuy(String userId, long quantity) {
        return submit(marketCommand(userId, OrderSide.BUY, quantity));
    }

    UUID marketSell(String userId, long quantity) {
        return submit(marketCommand(userId, OrderSide.SELL, quantity));
    }

    void cancel(String userId, UUID orderId) {
        batch(cancelCommand(userId, orderId));
    }

    BatchChanges batch(Command... commands) {
        return batch(List.of(commands));
    }

    BatchChanges batch(List<Command> commands) {
        lastChanges = processor.process(commands);
        for (var order : lastChanges.orders()) {
            ordersSeen.put(order.id(), order);
        }
        trades.addAll(lastChanges.trades());
        return lastChanges;
    }

    // ---- montagem de comandos ----

    Command depositCommand(String userId, Asset asset, String amount) {
        return new Command(nextCommandId++, userId, CommandType.DEPOSIT, null, null, null, null, null,
                asset, new BigDecimal(amount), CREATED_AT);
    }

    Command limitCommand(String userId, OrderSide side, String price, long quantity) {
        return new Command(nextCommandId++, userId, CommandType.NEW_ORDER, nextOrderId(), side,
                OrderType.LIMIT, new BigDecimal(price), quantity, null, null, CREATED_AT);
    }

    Command marketCommand(String userId, OrderSide side, long quantity) {
        return new Command(nextCommandId++, userId, CommandType.NEW_ORDER, nextOrderId(), side,
                OrderType.MARKET, null, quantity, null, null, CREATED_AT);
    }

    Command cancelCommand(String userId, UUID orderId) {
        return new Command(nextCommandId++, userId, CommandType.CANCEL_ORDER, orderId, null, null, null,
                null, null, null, CREATED_AT);
    }

    // ---- consultas ----

    Order order(UUID orderId) {
        var order = ordersSeen.get(orderId);
        if (order == null) {
            throw new AssertionError("Ordem nunca apareceu num lote: " + orderId);
        }
        return order;
    }

    BigDecimal available(String userId, Asset asset) {
        return wallets.find(userId, asset).map(Wallet::available).orElse(BigDecimal.ZERO);
    }

    BigDecimal reserved(String userId, Asset asset) {
        return wallets.find(userId, asset).map(Wallet::reserved).orElse(BigDecimal.ZERO);
    }

    boolean hasWallet(String userId, Asset asset) {
        return wallets.find(userId, asset).isPresent();
    }

    List<UUID> bookOrderIds(OrderSide side) {
        return book.ordersInPriority(side).stream().map(Order::id).toList();
    }

    List<Trade> trades() {
        return List.copyOf(trades);
    }

    List<Order> ordersSeen() {
        return List.copyOf(ordersSeen.values());
    }

    BatchChanges lastChanges() {
        return lastChanges;
    }

    OrderBook book() {
        return book;
    }

    Wallets wallets() {
        return wallets;
    }

    private UUID submit(Command command) {
        batch(command);
        return command.orderId();
    }

    private UUID nextOrderId() {
        return new UUID(0, nextOrderNumber++);
    }
}
