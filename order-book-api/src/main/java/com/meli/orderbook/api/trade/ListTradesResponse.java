package com.meli.orderbook.api.trade;

import com.meli.orderbook.api.common.Page;

import java.util.List;

/**
 * Saída de {@link ListTradesUseCase}: uma página do histórico de trades do
 * usuário, do mais recente para o mais antigo.
 *
 * @param items   os trades da página
 * @param page    o número da página
 * @param size    o tamanho de página pedido
 * @param hasNext {@code true} se existe próxima página
 */
public record ListTradesResponse(List<TradeItem> items, int page, int size, boolean hasNext) {

    /**
     * Monta a resposta a partir de uma página já convertida.
     *
     * @param trades a página de trades
     * @return a resposta
     */
    public static ListTradesResponse from(Page<TradeItem> trades) {
        return new ListTradesResponse(trades.items(), trades.page(), trades.size(), trades.hasNext());
    }
}
