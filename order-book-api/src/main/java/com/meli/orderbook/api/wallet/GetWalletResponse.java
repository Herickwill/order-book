package com.meli.orderbook.api.wallet;

import java.util.List;

/**
 * Saída de {@link GetWalletUseCase}: os saldos de um usuário, sempre com todos
 * os ativos, na ordem de declaração de {@code Asset}.
 *
 * @param userId   o dono da carteira
 * @param balances um saldo por ativo
 */
public record GetWalletResponse(String userId, List<BalanceItem> balances) {
}
