package com.meli.orderbook.api.wallet;

import com.meli.orderbook.api.wallet.repository.StatementEntryRow;
import com.meli.orderbook.shared.order.OrderSide;
import com.meli.orderbook.shared.wallet.Asset;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Uma linha do extrato, como item de {@link GetStatementResponse}.
 *
 * <p>{@code brlChange} e {@code vibChange} são o efeito da linha no saldo
 * total ({@code available + reserved}) de cada ativo. Somando todas as linhas
 * do extrato, chega-se ao total de cada ativo em {@code GET /wallets/{userId}}.
 * Reservas não aparecem: só movem valor entre {@code available} e
 * {@code reserved}, sem mudar o total.
 *
 * @param type       depósito, compra ou venda
 * @param occurredAt depósito: quando a API recebeu; trade: quando foi executado
 * @param brlChange  a variação de BRL; negativa numa compra
 * @param vibChange  a variação de VIB; negativa numa venda
 * @param depositId  o id do depósito enviado pelo cliente; nulo num trade
 * @param tradeId    o id do trade; nulo num depósito
 * @param orderId    a ordem do usuário no trade; nula num depósito
 * @param price      o preço do trade; nulo num depósito
 * @param quantity   a quantidade do trade; nula num depósito
 */
public record StatementEntryItem(
        StatementEntryType type,
        OffsetDateTime occurredAt,
        BigDecimal brlChange,
        long vibChange,
        String depositId,
        Long tradeId,
        UUID orderId,
        BigDecimal price,
        Long quantity
) {

    private static final BigDecimal NO_BRL_CHANGE = new BigDecimal("0.00");
    private static final long NO_VIB_CHANGE = 0;

    /**
     * Converte uma linha da consulta de extrato, calculando o sinal de cada
     * variação.
     *
     * @param entry a linha do extrato
     * @return o item
     */
    public static StatementEntryItem from(StatementEntryRow entry) {
        var deposit = entry.isDeposit();
        if (deposit) {
            return fromDeposit(entry);
        }

        return fromTrade(entry);
    }

    /**
     * O valor depositado entra inteiro no ativo do depósito. VIB é inteiro (o
     * CHECK de {@code commands} garante), então {@code longValueExact} não perde nada.
     */
    private static StatementEntryItem fromDeposit(StatementEntryRow entry) {
        var depositId = entry.idempotencyKey().substring(CreateDepositUseCase.IDEMPOTENCY_KEY_PREFIX.length());
        var brlDeposit = entry.asset() == Asset.BRL;

        var brlChange = brlDeposit ? entry.amount() : NO_BRL_CHANGE;
        var vibChange = brlDeposit ? NO_VIB_CHANGE : entry.amount().longValueExact();

        return new StatementEntryItem(
                StatementEntryType.DEPOSIT,
                entry.occurredAt(),
                brlChange,
                vibChange,
                depositId,
                null,
                null,
                null,
                null);
    }

    /** Compra: sai {@code preço * quantidade} de BRL e entra a quantidade de VIB. Venda: o contrário. */
    private static StatementEntryItem fromTrade(StatementEntryRow entry) {
        var quantity = entry.quantity();
        var tradeValue = entry.price().multiply(BigDecimal.valueOf(quantity));
        var buying = entry.side() == OrderSide.BUY;

        var type = buying ? StatementEntryType.BUY : StatementEntryType.SELL;
        var brlChange = buying ? tradeValue.negate() : tradeValue;
        var vibChange = buying ? quantity : -quantity;

        return new StatementEntryItem(
                type,
                entry.occurredAt(),
                brlChange,
                vibChange,
                null,
                entry.referenceId(),
                entry.orderId(),
                entry.price(),
                quantity);
    }
}
