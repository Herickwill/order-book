package com.meli.orderbook.api.wallet;

import com.meli.orderbook.api.common.ValidUserId;

/**
 * Entrada de {@link GetWalletUseCase}, montada a partir de {@code GET /wallets/{userId}}.
 *
 * @param userId o dono da carteira, do path
 */
public record GetWalletRequest(@ValidUserId String userId) {
}
