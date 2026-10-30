package com.meli.orderbook.worker.processing;

import com.meli.orderbook.worker.matching.Order;
import com.meli.orderbook.worker.matching.Trade;
import com.meli.orderbook.worker.settlement.Wallet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tudo que um lote altera e que vai para o banco numa única transação.
 *
 * <p>Ordens e carteiras são guardadas por referência: uma ordem tocada várias
 * vezes no lote aparece uma vez só, e o que se grava é o estado dela no fim do
 * lote. Trades e ids de comando ficam na ordem em que aconteceram.
 */
public final class BatchChanges {

    private final Map<UUID, Order> orders = new LinkedHashMap<>();
    private final List<Trade> trades = new ArrayList<>();
    private final List<Wallet> wallets = new ArrayList<>();
    private final List<Long> commandIds = new ArrayList<>();

    /**
     * Anota uma ordem criada ou alterada.
     *
     * @param order a ordem
     */
    public void recordOrder(Order order) {
        orders.putIfAbsent(order.id(), order);
    }

    /**
     * Anota um trade executado.
     *
     * @param trade o trade
     */
    public void recordTrade(Trade trade) {
        trades.add(trade);
    }

    /**
     * Anota as carteiras alteradas pelo lote. Chamado uma vez, no fim do
     * lote, com a lista já sem repetição que vem de {@code Wallets.drainTouched()}.
     *
     * @param changedWallets as carteiras alteradas
     */
    public void recordWallets(List<Wallet> changedWallets) {
        wallets.addAll(changedWallets);
    }

    /**
     * Anota um comando processado, a marcar como {@code PROCESSED}.
     *
     * @param commandId o id do comando
     */
    public void recordProcessed(long commandId) {
        commandIds.add(commandId);
    }

    /** {@return as ordens tocadas, na ordem do primeiro toque, com o estado atual} */
    public List<Order> orders() {
        return List.copyOf(orders.values());
    }

    /** {@return os trades, na ordem de execução} */
    public List<Trade> trades() {
        return List.copyOf(trades);
    }

    /** {@return as carteiras tocadas, na ordem da primeira alteração, com o estado atual} */
    public List<Wallet> wallets() {
        return List.copyOf(wallets);
    }

    /** {@return os ids dos comandos do lote, na ordem de processamento} */
    public List<Long> commandIds() {
        return List.copyOf(commandIds);
    }
}
