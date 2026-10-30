package com.meli.orderbook.worker.matching;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

/**
 * As ordens abertas de um preço, em ordem de chegada.
 *
 * <p>{@link LinkedHashMap} dá as duas coisas que o nível precisa: a cabeça da
 * fila (a mais antiga) em O(1) e a remoção de uma ordem qualquer em O(1) no
 * cancelamento. A ordem de inserção é a ordem de {@code seq}, porque as ordens
 * entram no nível na ordem em que o Worker as processa, e na reconstrução são
 * lidas por {@code seq}.
 */
public final class PriceLevel {

    private final BigDecimal price;
    private final LinkedHashMap<UUID, Order> orders = new LinkedHashMap<>();

    /**
     * Um nível vazio.
     *
     * @param price o preço do nível
     */
    public PriceLevel(BigDecimal price) {
        this.price = price;
    }

    /**
     * Coloca a ordem no fim da fila.
     *
     * @param order a ordem a enfileirar
     */
    public void append(Order order) {
        orders.putLast(order.id(), order);
    }

    /**
     * A ordem mais antiga do nível: a próxima a executar.
     *
     * @return a cabeça da fila
     * @throws java.util.NoSuchElementException se o nível está vazio
     */
    public Order head() {
        return orders.firstEntry().getValue();
    }

    /**
     * Tira a ordem do nível, esteja onde estiver na fila.
     *
     * @param orderId a ordem a remover
     * @throws IllegalStateException se a ordem não está no nível
     */
    public void remove(UUID orderId) {
        var removed = orders.remove(orderId);
        if (removed == null) {
            throw new IllegalStateException(
                    "Ordem ausente do nível. orderId=%s price=%s".formatted(orderId, price));
        }
    }

    /** {@return {@code true} se não sobrou nenhuma ordem} */
    public boolean isEmpty() {
        return orders.isEmpty();
    }

    /** {@return o preço do nível} */
    public BigDecimal price() {
        return price;
    }

    /** {@return as ordens do nível, da mais antiga para a mais nova} */
    public List<Order> orders() {
        return List.copyOf(orders.values());
    }
}
