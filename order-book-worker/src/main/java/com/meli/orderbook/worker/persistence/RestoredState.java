package com.meli.orderbook.worker.persistence;

import com.meli.orderbook.worker.matching.OrderBook;
import com.meli.orderbook.worker.settlement.Wallets;

/**
 * O estado em memória reconstruído do banco quando o Worker sobe.
 *
 * @param book        o livro, com as ordens abertas na ordem original das filas
 * @param wallets     todas as carteiras
 * @param lastSeq     o maior {@code seq} gravado; {@code 0} num banco vazio
 * @param openOrders  quantas ordens voltaram para o livro
 */
public record RestoredState(OrderBook book, Wallets wallets, long lastSeq, int openOrders) {
}
