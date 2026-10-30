package com.meli.orderbook.api.order;

import com.meli.orderbook.api.common.Page;

import java.util.List;

/**
 * Saída de {@link ListOpenOrdersUseCase}: uma página de ordens abertas, na
 * ordem em que serão executadas.
 *
 * @param items   as ordens da página
 * @param page    o número da página
 * @param size    o tamanho de página pedido
 * @param hasNext {@code true} se existe próxima página
 */
public record ListOpenOrdersResponse(List<OpenOrderItem> items, int page, int size, boolean hasNext) {

    /**
     * Monta a resposta a partir de uma página já convertida.
     *
     * @param openOrders a página de ordens abertas
     * @return a resposta
     */
    public static ListOpenOrdersResponse from(Page<OpenOrderItem> openOrders) {
        return new ListOpenOrdersResponse(
                openOrders.items(),
                openOrders.page(),
                openOrders.size(),
                openOrders.hasNext());
    }
}
