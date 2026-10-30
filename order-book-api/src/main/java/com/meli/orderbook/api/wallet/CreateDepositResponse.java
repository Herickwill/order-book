package com.meli.orderbook.api.wallet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.meli.orderbook.api.command.CommandRegistration;
import com.meli.orderbook.shared.command.CommandStatus;

/**
 * Saída de {@link CreateDepositUseCase}.
 *
 * <p>{@code status} é o estado do depósito no inbox. O saldo atualizado aparece
 * em {@code GET /wallets/{userId}} depois que o Worker processa o depósito.
 *
 * @param depositId a chave de idempotência enviada pelo cliente
 * @param status    o estado do depósito no inbox
 * @param created   {@code true} se esta requisição criou o depósito; fora do
 *                  JSON, porque o status HTTP já comunica isso
 */
public record CreateDepositResponse(String depositId, CommandStatus status, @JsonIgnore boolean created) {

    /**
     * Monta a resposta a partir do depósito registrado no inbox.
     *
     * @param depositId    a chave enviada pelo cliente, sem o prefixo interno
     * @param registration o depósito registrado
     * @return a resposta
     */
    public static CreateDepositResponse from(String depositId, CommandRegistration registration) {
        var command = registration.command();
        return new CreateDepositResponse(depositId, command.status(), registration.created());
    }
}
