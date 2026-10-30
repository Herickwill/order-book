package com.meli.orderbook.api.order;

import com.meli.orderbook.api.command.CommandService;
import org.springframework.stereotype.Component;

/**
 * Consulta o estado de uma ordem.
 *
 * <p>Procura primeiro em {@code orders}; se o Worker ainda não processou a
 * ordem, no inbox, onde ela aparece como {@code RECEIVED}.
 *
 * <p>As duas leituras não são atômicas. Se o Worker processar a ordem entre
 * elas, a resposta diz {@code RECEIVED} para uma ordem que acabou de ser
 * processada; a próxima consulta já traz o estado novo. É a consistência
 * eventual esperada de um inbox assíncrono.
 */
@Component
public class GetOrderUseCase {

    private final OrderService orderService;
    private final CommandService commandService;

    public GetOrderUseCase(OrderService orderService, CommandService commandService) {
        this.orderService = orderService;
        this.commandService = commandService;
    }

    /**
     * Devolve a ordem processada ou, se ainda estiver no inbox, a ordem recebida.
     *
     * @param request o id da ordem
     * @return o estado atual da ordem
     * @throws OrderNotFoundException se nenhuma ordem com este id foi enviada
     */
    public GetOrderResponse execute(GetOrderRequest request) {
        var orderId = request.orderId();

        var processedOrder = orderService.findById(orderId);
        if (processedOrder.isPresent()) {
            return GetOrderResponse.from(processedOrder.get());
        }

        var pendingOrder = commandService.findNewOrder(orderId);
        if (pendingOrder.isPresent()) {
            return GetOrderResponse.received(pendingOrder.get());
        }

        throw new OrderNotFoundException(orderId);
    }
}
