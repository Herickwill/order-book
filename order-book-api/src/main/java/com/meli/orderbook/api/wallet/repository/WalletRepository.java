package com.meli.orderbook.api.wallet.repository;

import java.util.List;

/** Leitura de {@code wallets}. A tabela é escrita apenas pelo Worker. */
public interface WalletRepository {

    /**
     * As carteiras do usuário, uma por ativo.
     *
     * @param userId o dono das carteiras
     * @return as carteiras; vazia se o usuário ainda não tem depósito processado
     */
    List<WalletRow> findByUserId(String userId);
}
