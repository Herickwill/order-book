package com.meli.orderbook.worker.settlement;

import com.meli.orderbook.shared.wallet.Asset;

import java.util.Objects;

/**
 * Identifica uma carteira: um usuário tem uma carteira por ativo. É a mesma
 * chave primária de {@code wallets}.
 *
 * @param userId o dono
 * @param asset  o ativo
 */
public record WalletKey(String userId, Asset asset) {

    /**
     * Rejeita chave incompleta.
     */
    public WalletKey {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(asset, "asset");
    }
}
