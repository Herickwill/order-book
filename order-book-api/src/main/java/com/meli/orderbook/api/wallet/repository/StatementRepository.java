package com.meli.orderbook.api.wallet.repository;

import java.util.List;

/**
 * Leitura do extrato: depósitos processados, de {@code commands}, e trades, de
 * {@code trades}. A primeira é escrita pela API e a segunda pelo Worker; aqui
 * só se lê.
 */
public interface StatementRepository {

    /**
     * Os depósitos processados e os trades do usuário, do mais recente para o
     * mais antigo.
     *
     * @param userId o usuário
     * @param limit  quantas linhas devolver
     * @param offset quantas linhas pular
     * @return as linhas do extrato
     */
    List<StatementEntryRow> findByUser(String userId, int limit, long offset);
}
