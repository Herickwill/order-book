package com.meli.orderbook.api.wallet;

import com.meli.orderbook.api.wallet.repository.WalletRow;
import com.meli.orderbook.shared.wallet.Asset;

import java.math.BigDecimal;

/**
 * O saldo de um ativo, como item de {@link GetWalletResponse}.
 *
 * <p>VIB é negociado em unidades inteiras, então sai sem casas decimais
 * ({@code 10}, e não {@code 10.00}), embora a coluna tenha duas. BRL mantém as
 * duas casas.
 *
 * @param asset     o ativo
 * @param available o que pode ser usado em novas ordens
 * @param reserved  o que está preso em ordens abertas
 */
public record BalanceItem(Asset asset, BigDecimal available, BigDecimal reserved) {

    private static final BigDecimal ZERO_BRL = new BigDecimal("0.00");

    /**
     * O saldo gravado pelo Worker.
     *
     * @param wallet a linha de {@code wallets}
     * @return o saldo, na escala de exibição do ativo
     */
    public static BalanceItem from(WalletRow wallet) {
        var asset = wallet.asset();
        var available = inDisplayScale(asset, wallet.available());
        var reserved = inDisplayScale(asset, wallet.reserved());

        return new BalanceItem(asset, available, reserved);
    }

    /**
     * Um ativo sem linha em {@code wallets}: o usuário ainda não tem depósito
     * processado nele.
     *
     * @param asset o ativo
     * @return o saldo zerado
     */
    public static BalanceItem empty(Asset asset) {
        var zero = inDisplayScale(asset, ZERO_BRL);
        return new BalanceItem(asset, zero, zero);
    }

    /**
     * {@code setScale(0)} sem modo de arredondamento é exato: se o valor de
     * VIB tivesse fração, lançaria exceção. O CHECK de {@code wallets} garante
     * que não tem.
     */
    private static BigDecimal inDisplayScale(Asset asset, BigDecimal value) {
        var isWholeUnits = asset == Asset.VIB;

        if (isWholeUnits) {
            return value.setScale(0);
        }

        return value;
    }
}
