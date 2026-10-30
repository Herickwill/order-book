package com.meli.orderbook.api.command;

import com.meli.orderbook.api.command.repository.CommandRepository;
import com.meli.orderbook.api.command.repository.CommandRow;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * O inbox de comandos. Pacote de suporte: não tem endpoint próprio, e é usado
 * pelas features {@code order} (nova ordem, cancelamento) e {@code wallet}
 * (depósito).
 */
@Service
public class CommandService {

    private final CommandRepository commandRepository;

    public CommandService(CommandRepository commandRepository) {
        this.commandRepository = commandRepository;
    }

    /**
     * Grava o comando ou, se a chave de idempotência já existe para o usuário,
     * devolve o original.
     *
     * @param newCommand o comando a gravar
     * @return o comando gravado e se ele foi criado agora
     * @throws IllegalStateException se o insert conflitou e o original não foi
     *                               encontrado, o que é uma invariante violada
     */
    public CommandRegistration register(CommandRow newCommand) {
        var insertedRows = commandRepository.insertIfAbsent(newCommand);
        var created = insertedRows > 0;

        if (created) {
            return CommandRegistration.newlyCreated(newCommand);
        }

        var originalCommand = findOriginal(newCommand.userId(), newCommand.idempotencyKey());
        return CommandRegistration.alreadyExisted(originalCommand);
    }

    /**
     * O comando de uma ordem que a API aceitou mas o Worker pode ainda não ter
     * processado.
     *
     * @param orderId o id da ordem
     * @return o comando {@code NEW_ORDER}, ou vazio se a ordem nunca foi enviada
     */
    public Optional<CommandRow> findNewOrder(UUID orderId) {
        return commandRepository.findNewOrderByOrderId(orderId);
    }

    /**
     * Só é chamado depois de um insert que conflitou. Quando o insert
     * conflita, o Postgres espera a transação concorrente terminar, então a
     * linha existe e está commitada. Não encontrá-la é uma invariante violada,
     * não um "não encontrado".
     */
    private CommandRow findOriginal(String userId, String idempotencyKey) {
        var originalCommand = commandRepository.findByIdempotency(userId, idempotencyKey);

        return originalCommand.orElseThrow(() -> new IllegalStateException(
                "Conflito de idempotência sem linha correspondente. userId=%s idempotencyKey=%s"
                        .formatted(userId, idempotencyKey)));
    }
}
