package com.meli.orderbook.worker.processing;

import com.meli.orderbook.shared.order.OrderSide;
import com.meli.orderbook.shared.order.RejectReason;
import com.meli.orderbook.shared.wallet.Asset;
import com.meli.orderbook.worker.matching.MatchResult;
import com.meli.orderbook.worker.matching.MatchingEngine;
import com.meli.orderbook.worker.matching.Order;
import com.meli.orderbook.worker.matching.OrderBook;
import com.meli.orderbook.worker.settlement.Settlement;

/**
 * Aplica {@code NEW_ORDER}: reserva o saldo, cruza com o livro, liquida os
 * trades e define o destino da ordem.
 *
 * <p>O fluxo muda conforme o tipo e o lado da ordem:
 *
 * <table>
 *   <caption>Diferenças entre os fluxos</caption>
 *   <tr><th></th><th>Reserva</th><th>Até que preço cruza</th><th>O que sobra</th></tr>
 *   <tr><td>{@code LIMIT}</td><td>tudo que pode gastar</td><td>o limite</td>
 *       <td>descansa no livro, com a reserva presa</td></tr>
 *   <tr><td>compra {@code MARKET}</td><td>todo o BRL disponível</td><td>qualquer um</td>
 *       <td>o BRL não gasto volta; a ordem não entra no livro</td></tr>
 *   <tr><td>venda {@code MARKET}</td><td>a quantidade de VIB</td><td>qualquer um</td>
 *       <td>o VIB não vendido volta; a ordem não entra no livro</td></tr>
 * </table>
 *
 * <p>Toda ordem recebe um {@code seq}, inclusive a rejeitada: {@code seq} é a
 * ordem oficial de chegada e é {@code NOT NULL UNIQUE} em {@code orders}.
 */
public final class NewOrderHandler {

    private final OrderBook book;
    private final MatchingEngine engine;
    private final Settlement settlement;
    private long lastSeq;

    /**
     * Cria o handler de {@code NEW_ORDER}.
     *
     * @param book       onde a sobra de uma {@code LIMIT} descansa
     * @param engine     o matching
     * @param settlement a reserva e a liquidação dos trades
     * @param lastSeq    o maior {@code seq} já atribuído; {@code 0} num banco vazio
     */
    public NewOrderHandler(OrderBook book, MatchingEngine engine, Settlement settlement, long lastSeq) {
        this.book = book;
        this.engine = engine;
        this.settlement = settlement;
        this.lastSeq = lastSeq;
    }

    /**
     * Aplica o comando e anota no lote a ordem nova, os trades e os makers
     * tocados.
     *
     * @param command um comando {@code NEW_ORDER}
     * @param changes o lote em montagem
     */
    public void handle(Command command, BatchChanges changes) {
        lastSeq++;
        var order = Order.accept(
                command.orderId(),
                command.userId(),
                lastSeq,
                command.side(),
                command.orderType(),
                command.price(),
                command.quantity(),
                command.createdAt());
        changes.recordOrder(order);

        switch (order.type()) {
            case LIMIT -> processLimit(order, changes);
            case MARKET -> processMarket(order, changes);
        }
    }

    /**
     * Reserva tudo que a ordem pode gastar, cruza até o preço limite e, se
     * sobrar quantidade, coloca a ordem no fim da fila do seu preço. A reserva
     * do restante continua presa enquanto ela estiver no livro.
     */
    private void processLimit(Order order, BatchChanges changes) {
        var reserved = settlement.reserveFor(order);
        if (!reserved) {
            order.reject(RejectReason.INSUFFICIENT_FUNDS);
            return;
        }

        var result = engine.match(order);
        settleTrades(order, result, changes);

        var hasRemaining = order.isOpen();
        if (hasRemaining) {
            book.add(order);
        }
    }

    private void processMarket(Order order, BatchChanges changes) {
        switch (order.side()) {
            case BUY -> processMarketBuy(order, changes);
            case SELL -> processMarketSell(order, changes);
        }
    }

    /**
     * A compra {@code MARKET} não tem preço, então não sabe quanto vai gastar.
     * Ela reserva todo o BRL disponível, compra enquanto esse valor pagar pelo
     * menos uma unidade e devolve o que não gastou.
     */
    private void processMarketBuy(Order order, BatchChanges changes) {
        var reservedBrl = settlement.reserveAllAvailable(order.userId(), Asset.BRL);
        if (reservedBrl.isEmpty()) {
            order.reject(RejectReason.INSUFFICIENT_FUNDS);
            return;
        }

        var budget = reservedBrl.get();
        var hadLiquidity = book.hasLiquidityFor(OrderSide.BUY);
        var result = engine.matchWithBudget(order, budget);
        settleTrades(order, result, changes);

        var unspent = budget.subtract(result.totalCost());
        var hasUnspent = unspent.signum() > 0;
        if (hasUnspent) {
            settlement.release(order.userId(), Asset.BRL, unspent);
        }

        concludeMarket(order, hadLiquidity);
    }

    /**
     * A venda {@code MARKET} reserva a quantidade inteira de VIB. Sem VIB
     * suficiente a ordem é rejeitada inteira; ela não vende só o que tem.
     */
    private void processMarketSell(Order order, BatchChanges changes) {
        var reserved = settlement.reserveFor(order);
        if (!reserved) {
            order.reject(RejectReason.INSUFFICIENT_FUNDS);
            return;
        }

        var hadLiquidity = book.hasLiquidityFor(OrderSide.SELL);
        var result = engine.match(order);
        settleTrades(order, result, changes);

        var hasRemaining = order.remaining() > 0;
        if (hasRemaining) {
            settlement.releaseRemaining(order);
        }

        concludeMarket(order, hadLiquidity);
    }

    /**
     * Liquida cada trade e anota no lote os trades e os makers tocados. Os
     * makers precisam ir para o lote porque um maker que zerou já saiu do
     * livro, e o estado final dele tem de ser gravado.
     */
    private void settleTrades(Order taker, MatchResult result, BatchChanges changes) {
        for (var trade : result.trades()) {
            settlement.settle(trade, taker);
            changes.recordTrade(trade);
        }
        for (var maker : result.touchedMakers()) {
            changes.recordOrder(maker);
        }
    }

    /**
     * Define o status final de uma {@code MARKET}, que nunca fica aberta:
     * {@code FILLED} se executou tudo, {@code CANCELLED} se executou parte e
     * {@code REJECTED} se não executou nada.
     *
     * <p>Na rejeição, o motivo depende de haver ordens do outro lado antes do
     * matching. Sem ordens: {@code NO_LIQUIDITY}. Com ordens e nada executado,
     * só pode ser uma compra cujo saldo não paga uma unidade:
     * {@code INSUFFICIENT_FUNDS}.
     */
    private static void concludeMarket(Order order, boolean hadLiquidity) {
        var filled = !order.isOpen();
        if (filled) {
            return;
        }

        var executedSomething = order.filledQuantity() > 0;
        if (executedSomething) {
            order.cancel();
            return;
        }

        if (hadLiquidity) {
            order.reject(RejectReason.INSUFFICIENT_FUNDS);
            return;
        }
        order.reject(RejectReason.NO_LIQUIDITY);
    }
}
