package com.meli.orderbook.api.order;

import com.meli.orderbook.api.command.CommandService;
import com.meli.orderbook.api.command.repository.CommandRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Registra uma nova ordem no inbox, de forma idempotente.
 *
 * <p>Não valida saldo e não casa nada: isso é responsabilidade do Worker.
 */
@Component
public class CreateOrderUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateOrderUseCase.class);

    /**
     * Separa as chaves de ordens das chaves de outros comandos do mesmo
     * usuário: sem o prefixo, um {@code clientOrderId} igual a um
     * {@code depositId} colidiria na constraint de idempotência.
     */
    private static final String IDEMPOTENCY_KEY_PREFIX = "order:";

    private final CommandService commandService;

    public CreateOrderUseCase(CommandService commandService) {
        this.commandService = commandService;
    }

    /**
     * Gera o id da ordem e grava o comando {@code NEW_ORDER}. Num reenvio, o
     * id gerado é descartado e volta o da ordem original.
     *
     * @param request a ordem enviada pelo cliente, já validada
     * @return o id da ordem e se ela foi criada agora
     */
    public CreateOrderResponse execute(CreateOrderRequest request) {
        var idempotencyKey = IDEMPOTENCY_KEY_PREFIX + request.clientOrderId();
        var orderId = UUID.randomUUID();

        var newCommand = CommandRow.newOrder(
                request.userId(),
                idempotencyKey,
                orderId,
                request.side(),
                request.type(),
                request.price(),
                request.quantity());

        var registration = commandService.register(newCommand);

        var isResend = !registration.created();
        if (isResend) {
            log.info("Ordem reenviada; devolvendo a original. userId={} clientOrderId={}",
                    request.userId(), request.clientOrderId());
        }

        return CreateOrderResponse.from(registration);
    }
}
