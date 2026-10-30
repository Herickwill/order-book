package com.meli.orderbook.api.trade.repository;

import java.util.List;

/** Leitura de {@code trades}. A tabela é escrita apenas pelo Worker. */
public interface TradeRepository {

    /**
     * Os trades do usuário, do mais recente para o mais antigo, como comprador
     * e como vendedor.
     *
     * @param userId o usuário
     * @param limit  quantas linhas devolver
     * @param offset quantas linhas pular
     * @return os trades do ponto de vista do usuário
     */
    List<UserTradeRow> findByUser(String userId, int limit, long offset);
}
