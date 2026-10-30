package com.meli.orderbook.api.order.repository;

import com.meli.orderbook.shared.order.OrderSide;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Leitura de {@code orders}. A tabela é escrita apenas pelo Worker. */
public interface OrderRepository {

    /**
     * Uma ordem pelo id.
     *
     * @param orderId o id da ordem
     * @return a ordem, ou vazio se ela não está em {@code orders}
     */
    Optional<OrderRow> findById(UUID orderId);

    /**
     * Ordens abertas de um lado do livro, na prioridade de execução: a
     * primeira é a próxima a ser casada.
     *
     * @param side   o lado do livro
     * @param userId filtra por usuário; {@code null} traz todos
     * @param limit  quantas linhas devolver
     * @param offset quantas linhas pular
     * @return as ordens abertas, já ordenadas
     */
    List<OrderRow> findOpen(OrderSide side, String userId, int limit, long offset);
}
