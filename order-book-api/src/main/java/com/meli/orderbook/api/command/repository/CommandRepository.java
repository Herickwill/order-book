package com.meli.orderbook.api.command.repository;

import java.util.Optional;
import java.util.UUID;

/** Acesso ao inbox de comandos, {@code commands}. A única tabela que a API escreve. */
public interface CommandRepository {

    /**
     * Insere o comando, a menos que já exista um com o mesmo
     * {@code (userId, idempotencyKey)}.
     *
     * @param command o comando a gravar
     * @return 1 se inseriu; 0 se a chave já existia
     */
    int insertIfAbsent(CommandRow command);

    /**
     * O comando gravado com esta chave de idempotência.
     *
     * @param userId         o dono do comando
     * @param idempotencyKey a chave já prefixada
     * @return o comando, ou vazio se a chave não existe
     */
    Optional<CommandRow> findByIdempotency(String userId, String idempotencyKey);

    /**
     * O comando {@code NEW_ORDER} que originou a ordem. Usado enquanto a ordem
     * ainda não aparece em {@code orders}.
     *
     * @param orderId o id da ordem
     * @return o comando, ou vazio se nenhuma ordem com este id foi enviada
     */
    Optional<CommandRow> findNewOrderByOrderId(UUID orderId);
}
