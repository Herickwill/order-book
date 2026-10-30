package com.meli.orderbook.api.wallet;

import com.meli.orderbook.api.command.CommandService;
import com.meli.orderbook.api.command.repository.CommandRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Registra um depósito no inbox.
 *
 * <p>O saldo não é alterado aqui: o Worker mantém as carteiras em memória e
 * sobrescreveria qualquer alteração feita por fora. Passando pelo inbox, o
 * depósito entra na mesma fila ordenada das ordens.
 */
@Component
public class CreateDepositUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateDepositUseCase.class);

    /**
     * Separa as chaves de depósitos das chaves de ordens do mesmo usuário. O
     * extrato tira este prefixo para devolver o {@code depositId}.
     */
    static final String IDEMPOTENCY_KEY_PREFIX = "deposit:";

    private final CommandService commandService;

    public CreateDepositUseCase(CommandService commandService) {
        this.commandService = commandService;
    }

    /**
     * Grava o comando {@code DEPOSIT}, ou devolve o original num reenvio.
     *
     * @param request o depósito enviado pelo cliente, já validado
     * @return o depósito e se ele foi criado agora
     */
    public CreateDepositResponse execute(CreateDepositRequest request) {
        var idempotencyKey = IDEMPOTENCY_KEY_PREFIX + request.depositId();
        var newCommand = CommandRow.deposit(request.userId(), idempotencyKey, request.asset(), request.amount());

        var registration = commandService.register(newCommand);

        var isResend = !registration.created();
        if (isResend) {
            log.info("Depósito reenviado; devolvendo o original. userId={} depositId={}",
                    request.userId(), request.depositId());
        }

        return CreateDepositResponse.from(request.depositId(), registration);
    }
}
