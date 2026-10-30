package com.meli.orderbook.worker.processing;

import com.meli.orderbook.worker.matching.MatchingEngine;
import com.meli.orderbook.worker.matching.OrderBook;
import com.meli.orderbook.worker.settlement.Settlement;
import com.meli.orderbook.worker.settlement.Wallets;

import java.util.List;

/**
 * Aplica um lote de comandos ao estado em memória, em ordem de id, e devolve
 * o que precisa ser gravado.
 *
 * <p>Cada tipo de comando tem um handler, escolhido por {@code switch}.
 *
 * <p>O processamento é determinístico: a mesma sequência de comandos sobre o
 * mesmo estado inicial produz o mesmo resultado. É isso que torna seguro
 * reprocessar, depois de uma queda, um lote cujo commit não aconteceu.
 */
public final class CommandProcessor {

    private final Wallets wallets;
    private final NewOrderHandler newOrderHandler;
    private final CancelOrderHandler cancelOrderHandler;
    private final DepositHandler depositHandler;

    /**
     * Monta o processador sobre um estado já carregado.
     *
     * @param book    o livro, vazio ou reconstruído do banco
     * @param wallets as carteiras, vazias ou reconstruídas do banco
     * @param lastSeq o maior {@code seq} já gravado; {@code 0} num banco vazio
     */
    public CommandProcessor(OrderBook book, Wallets wallets, long lastSeq) {
        this.wallets = wallets;

        var engine = new MatchingEngine(book);
        var settlement = new Settlement(wallets);
        this.newOrderHandler = new NewOrderHandler(book, engine, settlement, lastSeq);
        this.cancelOrderHandler = new CancelOrderHandler(book, settlement);
        this.depositHandler = new DepositHandler(settlement);
    }

    /**
     * Aplica os comandos na ordem recebida.
     *
     * @param commands os comandos do lote, em ordem de id
     * @return o estado final de tudo que o lote tocou
     */
    public BatchChanges process(List<Command> commands) {
        var changes = new BatchChanges();

        for (var command : commands) {
            switch (command.type()) {
                case NEW_ORDER -> newOrderHandler.handle(command, changes);
                case CANCEL_ORDER -> cancelOrderHandler.handle(command, changes);
                case DEPOSIT -> depositHandler.handle(command);
            }
            changes.recordProcessed(command.id());
        }

        var touchedWallets = wallets.drainTouched();
        changes.recordWallets(touchedWallets);
        return changes;
    }
}
