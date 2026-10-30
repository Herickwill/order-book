package com.meli.orderbook.api.wallet.repository;

import com.meli.orderbook.shared.wallet.Asset;

import java.math.BigDecimal;

/**
 * Uma linha de {@code wallets}: o saldo de um usuário em um ativo.
 *
 * @param userId    o dono da carteira
 * @param asset     o ativo
 * @param available o que pode ser usado em novas ordens
 * @param reserved  o que está preso em ordens abertas
 */
public record WalletRow(String userId, Asset asset, BigDecimal available, BigDecimal reserved) {
}
