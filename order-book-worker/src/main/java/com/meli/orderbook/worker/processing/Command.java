package com.meli.orderbook.worker.processing;

import com.meli.orderbook.shared.command.CommandType;
import com.meli.orderbook.shared.order.OrderSide;
import com.meli.orderbook.shared.order.OrderType;
import com.meli.orderbook.shared.wallet.Asset;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Um comando a processar, lido de {@code commands}. É a entrada do núcleo do
 * Worker.
 *
 * <p>Corresponde a uma linha da tabela, só com as colunas que o Worker usa. As
 * colunas que não pertencem ao tipo do comando ficam nulas, como no banco.
 *
 * @param id        o id do comando; define a ordem de processamento
 * @param userId    quem enviou
 * @param type      o tipo de comando
 * @param orderId   a ordem criada ou cancelada; nulo num depósito
 * @param side      lado da ordem; só em {@code NEW_ORDER}
 * @param orderType tipo da ordem; só em {@code NEW_ORDER}
 * @param price     preço limite; só em {@code NEW_ORDER} do tipo {@code LIMIT}
 * @param quantity  quantidade de VIB; só em {@code NEW_ORDER}
 * @param asset     ativo depositado; só em {@code DEPOSIT}
 * @param amount    valor depositado; só em {@code DEPOSIT}
 * @param createdAt o momento em que a API gravou o comando
 */
public record Command(
        long id,
        String userId,
        CommandType type,
        UUID orderId,
        OrderSide side,
        OrderType orderType,
        BigDecimal price,
        Long quantity,
        Asset asset,
        BigDecimal amount,
        OffsetDateTime createdAt
) {
}
