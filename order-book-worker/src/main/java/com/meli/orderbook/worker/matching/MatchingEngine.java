package com.meli.orderbook.worker.matching;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Cruza a ordem que chega (taker) com as ordens do livro (makers) e produz os
 * trades.
 *
 * <p>Regras do cruzamento:
 * <ul>
 *   <li>prioridade preço-tempo: melhor preço primeiro e, no mesmo preço, a
 *       ordem mais antiga primeiro;</li>
 *   <li>o trade sai sempre ao preço do maker, quem já estava no livro;</li>
 *   <li>um maker que zera sai do livro na hora.</li>
 * </ul>
 *
 * <p>Esta classe não coloca o taker no livro e não mexe em saldo. Quem faz
 * isso é o {@code NewOrderHandler}, que chama o {@code Settlement} para cada
 * trade devolvido.
 */
public final class MatchingEngine {

    private final OrderBook book;

    /**
     * Cria o matching sobre um livro.
     *
     * @param book o livro sobre o qual o matching opera
     */
    public MatchingEngine(OrderBook book) {
        this.book = book;
    }

    /**
     * Executa a ordem sem teto de gasto. Usado pela {@code LIMIT}, que já
     * reservou tudo o que pode gastar, e pela venda {@code MARKET}, que recebe
     * dinheiro em vez de pagar.
     *
     * @param taker a ordem que chega, aberta
     * @return os trades e os makers tocados
     */
    public MatchResult match(Order taker) {
        return execute(taker, null);
    }

    /**
     * Executa a ordem gastando no máximo {@code budget} em BRL. Usado só pela
     * compra {@code MARKET}: ela não tem preço limite, então o que a impede de
     * gastar mais do que tem é este teto.
     *
     * @param taker  a compra {@code MARKET}, aberta
     * @param budget o BRL reservado para a ordem
     * @return os trades e os makers tocados
     */
    public MatchResult matchWithBudget(Order taker, BigDecimal budget) {
        Objects.requireNonNull(budget, "budget");
        return execute(taker, budget);
    }

    /**
     * O loop do matching. Para quando a ordem zera, quando o lado contrário
     * esvazia, quando o melhor preço passa do limite da ordem ou quando o
     * teto não paga uma unidade.
     *
     * <p>Nos dois últimos casos parar é seguro porque os níveis seguintes são
     * piores para o taker: se o melhor preço não serve, nenhum outro serve.
     *
     * @param budget o teto de gasto; {@code null} quando não há teto
     */
    private MatchResult execute(Order taker, BigDecimal budget) {
        var trades = new ArrayList<Trade>();
        var touchedMakers = new ArrayList<Order>();
        var spent = BigDecimal.ZERO;

        while (taker.remaining() > 0) {
            var bestLevel = book.bestOpposite(taker.side());
            if (bestLevel.isEmpty()) {
                break;
            }

            var level = bestLevel.get();
            var acceptsPrice = taker.acceptsPrice(level.price());
            if (!acceptsPrice) {
                break;
            }

            var maker = level.head();
            var quantity = Math.min(taker.remaining(), maker.remaining());

            var hasBudget = budget != null;
            if (hasBudget) {
                var unspent = budget.subtract(spent);
                var affordable = affordableUnits(unspent, maker.price());
                quantity = Math.min(quantity, affordable);
            }

            if (quantity == 0) {
                break;
            }

            var trade = Trade.between(taker, maker, quantity);
            taker.fill(quantity);
            maker.fill(quantity);
            spent = spent.add(trade.cost());
            trades.add(trade);
            touchedMakers.add(maker);

            var makerOpen = maker.isOpen();
            if (!makerOpen) {
                book.remove(maker);
            }
        }

        return new MatchResult(trades, touchedMakers);
    }

    /**
     * Quantas unidades inteiras o valor paga a este preço. A divisão inteira
     * ({@code divideToIntegralValue}) é exata e dispensa {@code RoundingMode}:
     * 60,00 a 13,00 paga 4 unidades, e os 8,00 restantes voltam ao comprador.
     */
    private static long affordableUnits(BigDecimal amount, BigDecimal price) {
        var units = amount.divideToIntegralValue(price);
        return units.longValueExact();
    }
}
