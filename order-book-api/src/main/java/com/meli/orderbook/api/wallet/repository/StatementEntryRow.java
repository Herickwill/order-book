package com.meli.orderbook.api.wallet.repository;

import com.meli.orderbook.shared.order.OrderSide;
import com.meli.orderbook.shared.wallet.Asset;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Uma linha da consulta de extrato: um depósito processado ou um trade do
 * usuário. Não é linha de tabela; junta {@code commands} e {@code trades}.
 *
 * <p>Num depósito, {@code side}, {@code orderId}, {@code price} e
 * {@code quantity} são nulos. Num trade, {@code idempotencyKey}, {@code asset}
 * e {@code amount} são nulos, e {@code side} é o lado do usuário. Um self-trade
 * aparece duas vezes, uma como BUY e outra como SELL.
 *
 * @param referenceId    o id do comando de depósito ou do trade
 * @param occurredAt     depósito: quando a API gravou o comando; trade: quando foi gravado
 * @param side           o lado do usuário no trade; nulo num depósito
 * @param orderId        a ordem do usuário no trade; nulo num depósito
 * @param price          o preço do trade; nulo num depósito
 * @param quantity       a quantidade do trade; nulo num depósito
 * @param idempotencyKey a chave do depósito, com o prefixo; nula num trade
 * @param asset          o ativo depositado; nulo num trade
 * @param amount         o valor depositado; nulo num trade
 */
public record StatementEntryRow(
        long referenceId,
        OffsetDateTime occurredAt,
        OrderSide side,
        UUID orderId,
        BigDecimal price,
        Long quantity,
        String idempotencyKey,
        Asset asset,
        BigDecimal amount
) {

    /** {@return {@code true} se a linha é um depósito, e não um trade} */
    public boolean isDeposit() {
        return side == null;
    }
}
