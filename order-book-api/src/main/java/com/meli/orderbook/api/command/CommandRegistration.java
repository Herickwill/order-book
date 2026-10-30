package com.meli.orderbook.api.command;

import com.meli.orderbook.api.command.repository.CommandRow;

/**
 * Resultado de registrar um comando no inbox.
 *
 * @param command o comando gravado: o novo ou, num reenvio, o original
 * @param created {@code true} se esta chamada criou o comando
 */
public record CommandRegistration(CommandRow command, boolean created) {

    /**
     * O comando foi gravado agora.
     *
     * @param command o comando recém-gravado
     * @return o registro com {@code created = true}
     */
    public static CommandRegistration newlyCreated(CommandRow command) {
        return new CommandRegistration(command, true);
    }

    /**
     * A chave de idempotência já existia: é um reenvio.
     *
     * @param originalCommand o comando gravado no primeiro envio
     * @return o registro com {@code created = false}
     */
    public static CommandRegistration alreadyExisted(CommandRow originalCommand) {
        return new CommandRegistration(originalCommand, false);
    }
}
