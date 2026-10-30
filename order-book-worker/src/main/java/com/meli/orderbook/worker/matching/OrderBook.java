package com.meli.orderbook.worker.matching;

import com.meli.orderbook.shared.order.OrderSide;
import com.meli.orderbook.shared.order.OrderType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

/**
 * O livro de ofertas: as ordens {@code LIMIT} abertas, por lado e preço.
 *
 * <p>A prioridade é o preço e, no mesmo preço, a chegada. {@code bids} fica em
 * ordem decrescente (maior compra primeiro) e {@code asks} em ordem crescente
 * (menor venda primeiro). O {@link TreeMap} compara as chaves com
 * {@code compareTo}, então {@code 10.5} e {@code 10.50} são o mesmo nível. Um
 * nível que esvazia sai do mapa, e por isso o primeiro nível de cada lado é
 * sempre o melhor preço com ordem.
 *
 * <p>Só a thread do loop acessa o livro, por isso nada aqui é sincronizado.
 */
public final class OrderBook {

    private final TreeMap<BigDecimal, PriceLevel> bids = new TreeMap<>(Comparator.reverseOrder());
    private final TreeMap<BigDecimal, PriceLevel> asks = new TreeMap<>();
    private final Map<UUID, Order> openOrders = new HashMap<>();

    /**
     * Põe a ordem no fim da fila do seu preço. Usado quando uma {@code LIMIT}
     * sobra depois do matching e na reconstrução do estado.
     *
     * @param order uma ordem {@code LIMIT} aberta com restante positivo
     * @throws IllegalArgumentException se a ordem não pode descansar no livro
     * @throws IllegalStateException    se a ordem já está no livro
     */
    public void add(Order order) {
        var limit = order.type() == OrderType.LIMIT;
        var open = order.isOpen();
        if (!limit || !open) {
            throw new IllegalArgumentException(
                    "Só ordem LIMIT aberta descansa no livro. %s".formatted(order));
        }

        var alreadyInBook = openOrders.containsKey(order.id());
        if (alreadyInBook) {
            throw new IllegalStateException("Ordem já está no livro. %s".formatted(order));
        }

        var side = sideOf(order.side());
        var level = side.computeIfAbsent(order.price(), PriceLevel::new);
        level.append(order);
        openOrders.put(order.id(), order);
    }

    /**
     * Tira a ordem do livro. O nível que esvazia sai junto.
     *
     * @param order uma ordem que está no livro
     * @throws IllegalStateException se a ordem não está no livro
     */
    public void remove(Order order) {
        var removed = openOrders.remove(order.id());
        if (removed == null) {
            throw new IllegalStateException("Ordem ausente do livro. %s".formatted(order));
        }

        var side = sideOf(order.side());
        var level = side.get(order.price());
        level.remove(order.id());

        var emptied = level.isEmpty();
        if (emptied) {
            side.remove(order.price());
        }
    }

    /**
     * A ordem aberta com este id.
     *
     * @param orderId o id procurado
     * @return a ordem, ou vazio se ela não está no livro
     */
    public Optional<Order> find(UUID orderId) {
        var order = openOrders.get(orderId);
        return Optional.ofNullable(order);
    }

    /**
     * O melhor nível do lado contrário ao de quem chega: o menor ask para uma
     * compra, o maior bid para uma venda.
     *
     * @param takerSide o lado da ordem que chega
     * @return o melhor nível, ou vazio se o lado contrário não tem ordens
     */
    public Optional<PriceLevel> bestOpposite(OrderSide takerSide) {
        var oppositeSide = takerSide == OrderSide.BUY ? asks : bids;
        var best = oppositeSide.firstEntry();

        if (best == null) {
            return Optional.empty();
        }
        return Optional.of(best.getValue());
    }

    /**
     * Se o lado contrário ao de quem chega tem alguma ordem.
     *
     * @param takerSide o lado da ordem que chega
     * @return {@code true} se há liquidez para cruzar
     */
    public boolean hasLiquidityFor(OrderSide takerSide) {
        var best = bestOpposite(takerSide);
        return best.isPresent();
    }

    /**
     * O melhor preço de um lado.
     *
     * @param side o lado consultado
     * @return o maior bid ou o menor ask; vazio se o lado não tem ordens
     */
    public Optional<BigDecimal> bestPrice(OrderSide side) {
        var levels = sideOf(side);
        var best = levels.firstEntry();

        if (best == null) {
            return Optional.empty();
        }
        return Optional.of(best.getKey());
    }

    /**
     * As ordens abertas de um lado, na prioridade de execução: melhor preço
     * primeiro e, no mesmo preço, a mais antiga primeiro.
     *
     * @param side o lado consultado
     * @return uma cópia; alterá-la não altera o livro
     */
    public List<Order> ordersInPriority(OrderSide side) {
        var levels = sideOf(side);
        var result = new ArrayList<Order>();

        for (var level : levels.values()) {
            result.addAll(level.orders());
        }
        return result;
    }

    private TreeMap<BigDecimal, PriceLevel> sideOf(OrderSide side) {
        return side == OrderSide.BUY ? bids : asks;
    }
}
