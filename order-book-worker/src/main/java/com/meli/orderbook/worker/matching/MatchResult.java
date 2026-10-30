package com.meli.orderbook.worker.matching;

import java.math.BigDecimal;
import java.util.List;

/**
 * O que uma rodada de matching produziu.
 *
 * <p>Os makers vêm junto porque um maker totalmente executado sai do livro.
 * Sem esta lista, quem chamou não teria como gravar o estado final dele.
 *
 * @param trades        os trades, na ordem em que aconteceram
 * @param touchedMakers as ordens do livro que executaram, na mesma ordem
 */
public record MatchResult(List<Trade> trades, List<Order> touchedMakers) {

    /**
     * Guarda cópias imutáveis das listas.
     */
    public MatchResult {
        trades = List.copyOf(trades);
        touchedMakers = List.copyOf(touchedMakers);
    }

    /**
     * O valor total pago pelos trades: soma de {@code preço * quantidade}.
     *
     * @return o total em BRL; zero se não houve trade
     */
    public BigDecimal totalCost() {
        var total = BigDecimal.ZERO;
        for (var trade : trades) {
            total = total.add(trade.cost());
        }
        return total;
    }
}
