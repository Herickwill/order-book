package com.meli.orderbook.api.trade;

import org.springframework.stereotype.Component;

/** Consulta o histórico de trades de um usuário, paginado: a rastreabilidade das transações. */
@Component
public class ListTradesUseCase {

    private final TradeService tradeService;

    public ListTradesUseCase(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    /**
     * Busca a página pedida e a converte em resposta.
     *
     * @param request o usuário e a paginação
     * @return a página de trades
     */
    public ListTradesResponse execute(ListTradesRequest request) {
        var trades = tradeService.findPage(request.userId(), request.page(), request.size());

        var items = trades.map(TradeItem::from);
        return ListTradesResponse.from(items);
    }
}
