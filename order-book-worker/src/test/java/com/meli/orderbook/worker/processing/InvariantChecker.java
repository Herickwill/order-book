package com.meli.orderbook.worker.processing;

import com.meli.orderbook.shared.order.OrderSide;
import com.meli.orderbook.shared.order.OrderType;
import com.meli.orderbook.shared.wallet.Asset;
import com.meli.orderbook.worker.matching.Order;
import com.meli.orderbook.worker.matching.OrderBook;
import com.meli.orderbook.worker.matching.Trade;
import com.meli.orderbook.worker.settlement.Wallet;
import com.meli.orderbook.worker.settlement.WalletKey;
import com.meli.orderbook.worker.settlement.Wallets;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Confere as cinco invariantes do livro: conservação, lastro das reservas,
 * saldo não negativo, livro não cruzado e execução consistente.
 *
 * <p>Escrito a partir do enunciado delas, não da lógica do motor: o esperado é
 * recalculado do histórico (depósitos aceitos e trades) e das ordens abertas,
 * e só então comparado com as carteiras. Se o verificador usasse a contabilidade
 * do próprio motor, concordaria com os bugs dele.
 */
final class InvariantChecker {

    private final Map<Asset, BigDecimal> deposited = new EnumMap<>(Asset.class);
    private final Map<UUID, Long> tradedQuantity = new HashMap<>();
    private final Map<UUID, Order> orders = new LinkedHashMap<>();

    InvariantChecker() {
        deposited.put(Asset.BRL, BigDecimal.ZERO);
        deposited.put(Asset.VIB, BigDecimal.ZERO);
    }

    /** Um depósito que o gerador sabe que cabe na coluna (valores pequenos). */
    void recordDeposit(Asset asset, BigDecimal amount) {
        deposited.merge(asset, amount, BigDecimal::add);
    }

    void recordChanges(BatchChanges changes) {
        for (var order : changes.orders()) {
            orders.put(order.id(), order);
        }
        for (var trade : changes.trades()) {
            addTraded(trade.buyOrderId(), trade);
            addTraded(trade.sellOrderId(), trade);
        }
    }

    /**
     * @return a primeira violação encontrada, ou {@code null} se todas valem
     */
    String firstViolation(OrderBook book, Wallets wallets) {
        var wallet = wallets.all();

        var conservation = conservation(wallet);
        if (conservation != null) {
            return conservation;
        }
        var backing = backing(book, wallets);
        if (backing != null) {
            return backing;
        }
        var nonNegative = nonNegative(wallet);
        if (nonNegative != null) {
            return nonNegative;
        }
        var crossed = notCrossed(book);
        if (crossed != null) {
            return crossed;
        }
        return executionConsistency(book);
    }

    /** 1. Para cada ativo, soma de available + reserved = soma dos depósitos. */
    private String conservation(List<Wallet> wallets) {
        for (var asset : Asset.values()) {
            var total = BigDecimal.ZERO;
            for (var wallet : wallets) {
                if (wallet.asset() == asset) {
                    total = total.add(wallet.available()).add(wallet.reserved());
                }
            }
            var expected = deposited.get(asset);
            if (total.compareTo(expected) != 0) {
                return "Conservação violada em %s: carteiras=%s depósitos=%s".formatted(asset, total, expected);
            }
        }
        return null;
    }

    /** 2. BRL.reserved = soma de preço * restante das compras abertas; VIB.reserved = soma do restante das vendas. */
    private String backing(OrderBook book, Wallets wallets) {
        var expected = new HashMap<WalletKey, BigDecimal>();
        for (var buy : book.ordersInPriority(OrderSide.BUY)) {
            var locked = buy.price().multiply(BigDecimal.valueOf(buy.remaining()));
            expected.merge(new WalletKey(buy.userId(), Asset.BRL), locked, BigDecimal::add);
        }
        for (var sell : book.ordersInPriority(OrderSide.SELL)) {
            var locked = BigDecimal.valueOf(sell.remaining());
            expected.merge(new WalletKey(sell.userId(), Asset.VIB), locked, BigDecimal::add);
        }

        for (var wallet : wallets.all()) {
            var expectedReserved = expected.getOrDefault(wallet.key(), BigDecimal.ZERO);
            if (wallet.reserved().compareTo(expectedReserved) != 0) {
                return "Lastro violado: %s esperado reserved=%s".formatted(wallet, expectedReserved);
            }
        }
        for (var entry : expected.entrySet()) {
            var walletExists = wallets.find(entry.getKey().userId(), entry.getKey().asset()).isPresent();
            if (!walletExists) {
                return "Lastro violado: ordem aberta sem carteira " + entry.getKey();
            }
        }
        return null;
    }

    /** 3. Nenhum saldo negativo. */
    private String nonNegative(List<Wallet> wallets) {
        for (var wallet : wallets) {
            if (wallet.available().signum() < 0 || wallet.reserved().signum() < 0) {
                return "Saldo negativo: " + wallet;
            }
        }
        return null;
    }

    /** 4. Melhor bid < melhor ask. */
    private String notCrossed(OrderBook book) {
        var bestBid = book.bestPrice(OrderSide.BUY);
        var bestAsk = book.bestPrice(OrderSide.SELL);
        if (bestBid.isPresent() && bestAsk.isPresent() && bestBid.get().compareTo(bestAsk.get()) >= 0) {
            return "Livro cruzado: bid=%s ask=%s".formatted(bestBid.get(), bestAsk.get());
        }
        return null;
    }

    /** 5. filled = soma dos trades da ordem, 0 <= filled <= quantity; e o livro tem exatamente as LIMIT abertas. */
    private String executionConsistency(OrderBook book) {
        var openSeen = 0;
        for (var order : orders.values()) {
            var traded = tradedQuantity.getOrDefault(order.id(), 0L);
            if (order.filledQuantity() != traded) {
                return "Execução inconsistente: %s trades=%d".formatted(order, traded);
            }
            if (order.filledQuantity() < 0 || order.filledQuantity() > order.quantity()) {
                return "filled fora do intervalo: " + order;
            }
            var open = order.status().isOpen();
            if (open && order.type() == OrderType.MARKET) {
                return "MARKET aberta depois do comando: " + order;
            }
            if (open) {
                openSeen++;
                var inBook = book.find(order.id()).isPresent();
                if (!inBook) {
                    return "Ordem aberta fora do livro: " + order;
                }
            }
        }

        var inBook = book.ordersInPriority(OrderSide.BUY).size() + book.ordersInPriority(OrderSide.SELL).size();
        if (inBook != openSeen) {
            return "Livro com %d ordens, mas %d abertas conhecidas".formatted(inBook, openSeen);
        }
        return null;
    }

    private void addTraded(UUID orderId, Trade trade) {
        tradedQuantity.merge(orderId, trade.quantity(), Long::sum);
    }
}
