package com.meli.orderbook.api.order;

import org.springframework.stereotype.Component;

/**
 * Lista as ordens abertas de um lado do livro, opcionalmente de um só usuário,
 * na prioridade de execução: a primeira é a próxima a ser casada.
 */
@Component
public class ListOpenOrdersUseCase {

    private final OrderService orderService;

    public ListOpenOrdersUseCase(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Busca a página pedida e a converte em resposta.
     *
     * @param request o lado do livro, o filtro de usuário e a paginação
     * @return a página de ordens abertas
     */
    public ListOpenOrdersResponse execute(ListOpenOrdersRequest request) {
        var openOrders = orderService.findOpenPage(request.side(), request.userId(), request.page(), request.size());

        var items = openOrders.map(OpenOrderItem::from);
        return ListOpenOrdersResponse.from(items);
    }
}
