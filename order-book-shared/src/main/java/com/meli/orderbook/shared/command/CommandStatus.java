package com.meli.orderbook.shared.command;

/**
 * Estado de um comando na tabela {@code commands} (o inbox).
 *
 * <p>A API grava o comando como {@code RECEIVED}. O Worker marca
 * {@code PROCESSED} na mesma transação em que grava os efeitos do comando, e
 * por isso um comando nunca é aplicado duas vezes.
 */
public enum CommandStatus {
    RECEIVED,
    PROCESSED
}
