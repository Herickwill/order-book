package com.meli.orderbook.api.order;

import com.meli.orderbook.api.command.CommandService;
import com.meli.orderbook.api.command.repository.CommandRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Registra um pedido de cancelamento no inbox.
 *
 * <p>Não decide se a ordem pode ser cancelada: só o Worker sabe se ela ainda
 * está aberta e se pertence ao usuário.
 */
@Component
public class CancelOrderUseCase {

    private static final Logger log = LoggerFactory.getLogger(CancelOrderUseCase.class);

    /** A chave deriva do orderId: pedir o cancelamento duas vezes gera um único comando. */
    private static final String IDEMPOTENCY_KEY_PREFIX = "cancel:";

    private final CommandService commandService;

    public CancelOrderUseCase(CommandService commandService) {
        this.commandService = commandService;
    }

    /**
     * Grava o comando {@code CANCEL_ORDER}, ou devolve o original se esta
     * ordem já teve o cancelamento pedido por este usuário.
     *
     * @param request a ordem e quem pede o cancelamento
     * @return o pedido de cancelamento e se ele foi criado agora
     */
    public CancelOrderResponse execute(CancelOrderRequest request) {
        var idempotencyKey = IDEMPOTENCY_KEY_PREFIX + request.orderId();
        var newCommand = CommandRow.cancelOrder(request.userId(), idempotencyKey, request.orderId());

        var registration = commandService.register(newCommand);

        var isResend = !registration.created();
        if (isResend) {
            log.info("Cancelamento reenviado; devolvendo o original. userId={} orderId={}",
                    request.userId(), request.orderId());
        }

        return CancelOrderResponse.from(registration);
    }
}
