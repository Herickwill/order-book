package com.meli.orderbook.api.command.repository;

import com.meli.orderbook.shared.command.CommandStatus;
import com.meli.orderbook.shared.command.CommandType;
import com.meli.orderbook.shared.order.OrderSide;
import com.meli.orderbook.shared.order.OrderType;
import com.meli.orderbook.shared.wallet.Asset;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Uma linha de {@code commands}, coluna por coluna, na ordem do schema.
 *
 * <p>A tabela guarda os três tipos de comando; as colunas que não pertencem ao
 * tipo da linha ficam nulas. {@code id} e {@code createdAt} são gerados pelo
 * banco e ficam nulos numa linha ainda não gravada.
 *
 * @param id             identificador gerado pelo banco; define a ordem de processamento
 * @param userId         quem enviou o comando
 * @param idempotencyKey chave de idempotência, prefixada pelo tipo de comando
 * @param type           o tipo de comando
 * @param status         {@code RECEIVED} até o Worker processar o comando
 * @param orderId        a ordem criada ou cancelada; nulo num depósito
 * @param side           lado da ordem; só em {@code NEW_ORDER}
 * @param orderType      tipo da ordem; só em {@code NEW_ORDER}
 * @param price          preço limite; só em {@code NEW_ORDER} do tipo {@code LIMIT}
 * @param quantity       quantidade de VIB; só em {@code NEW_ORDER}
 * @param asset          ativo depositado; só em {@code DEPOSIT}
 * @param amount         valor depositado; só em {@code DEPOSIT}
 * @param createdAt      momento da gravação
 */
public record CommandRow(
        Long id,
        String userId,
        String idempotencyKey,
        CommandType type,
        CommandStatus status,
        UUID orderId,
        OrderSide side,
        OrderType orderType,
        BigDecimal price,
        Long quantity,
        Asset asset,
        BigDecimal amount,
        OffsetDateTime createdAt
) {

    /**
     * Um novo comando {@code NEW_ORDER}, ainda não gravado.
     *
     * @param userId         quem envia a ordem
     * @param idempotencyKey a chave já prefixada
     * @param orderId        o id gerado para a ordem
     * @param side           o lado da ordem
     * @param orderType      o tipo da ordem
     * @param price          o preço limite; nulo numa ordem {@code MARKET}
     * @param quantity       a quantidade de VIB
     * @return a linha a inserir
     */
    public static CommandRow newOrder(
            String userId,
            String idempotencyKey,
            UUID orderId,
            OrderSide side,
            OrderType orderType,
            BigDecimal price,
            Long quantity) {

        return new CommandRow(
                null,
                userId,
                idempotencyKey,
                CommandType.NEW_ORDER,
                CommandStatus.RECEIVED,
                orderId,
                side,
                orderType,
                price,
                quantity,
                null,
                null,
                null);
    }

    /**
     * Um novo comando {@code CANCEL_ORDER}, ainda não gravado.
     *
     * @param userId         quem pede o cancelamento
     * @param idempotencyKey a chave já prefixada
     * @param orderId        a ordem a cancelar
     * @return a linha a inserir
     */
    public static CommandRow cancelOrder(String userId, String idempotencyKey, UUID orderId) {
        return new CommandRow(
                null,
                userId,
                idempotencyKey,
                CommandType.CANCEL_ORDER,
                CommandStatus.RECEIVED,
                orderId,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    /**
     * Um novo comando {@code DEPOSIT}, ainda não gravado.
     *
     * @param userId         o dono da carteira
     * @param idempotencyKey a chave já prefixada
     * @param asset          o ativo depositado
     * @param amount         o valor depositado
     * @return a linha a inserir
     */
    public static CommandRow deposit(String userId, String idempotencyKey, Asset asset, BigDecimal amount) {
        return new CommandRow(
                null,
                userId,
                idempotencyKey,
                CommandType.DEPOSIT,
                CommandStatus.RECEIVED,
                null,
                null,
                null,
                null,
                null,
                asset,
                amount,
                null);
    }
}
