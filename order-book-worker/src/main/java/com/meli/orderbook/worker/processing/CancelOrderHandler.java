package com.meli.orderbook.worker.processing;

import com.meli.orderbook.worker.matching.OrderBook;
import com.meli.orderbook.worker.settlement.Settlement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Aplica {@code CANCEL_ORDER}: tira a ordem do livro e devolve a reserva do
 * restante.
 *
 * <p>Cancelar uma ordem que não está no livro (já executada, já cancelada,
 * {@code MARKET} ou inexistente) ou que é de outro usuário não é erro: o
 * pedido chegou tarde ou não veio do dono. O comando é ignorado com log
 * {@code WARN} e marcado como processado.
 */
public final class CancelOrderHandler {

    private static final Logger log = LoggerFactory.getLogger(CancelOrderHandler.class);

    private final OrderBook book;
    private final Settlement settlement;

    /**
     * Cria o handler de {@code CANCEL_ORDER}.
     *
     * @param book       onde a ordem aberta está
     * @param settlement onde a reserva é devolvida
     */
    public CancelOrderHandler(OrderBook book, Settlement settlement) {
        this.book = book;
        this.settlement = settlement;
    }

    /**
     * Aplica o comando e anota no lote a ordem cancelada.
     *
     * @param command um comando {@code CANCEL_ORDER}
     * @param changes o lote em montagem
     */
    public void handle(Command command, BatchChanges changes) {
        var found = book.find(command.orderId());
        if (found.isEmpty()) {
            log.warn("Cancelamento ignorado: ordem fora do livro. orderId={} userId={}",
                    command.orderId(), command.userId());
            return;
        }

        var order = found.get();
        var owner = order.userId().equals(command.userId());
        if (!owner) {
            log.warn("Cancelamento ignorado: ordem de outro usuário. orderId={} userId={}",
                    command.orderId(), command.userId());
            return;
        }

        book.remove(order);
        settlement.releaseRemaining(order);
        order.cancel();
        changes.recordOrder(order);
    }
}
