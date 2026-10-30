package com.meli.orderbook.api.monitoring.repository;

import com.meli.orderbook.shared.order.OrderSide;

/**
 * Consultas pontuais sobre o estado do motor, para métricas e health. Todas
 * são somente leitura e usam índices parciais ou o catálogo do Postgres.
 */
public interface MonitoringRepository {

    /** {@return quantos comandos estão {@code RECEIVED}, esperando o Worker} */
    long countPendingCommands();

    /** {@return há quantos segundos o comando pendente mais antigo chegou; zero se a fila está vazia} */
    double oldestPendingSeconds();

    /**
     * Quantas ordens abertas há num lado do livro.
     *
     * @param side o lado
     * @return as ordens {@code NEW} e {@code PARTIALLY_FILLED} do lado
     */
    long countOpenOrders(OrderSide side);

    /**
     * Quanto VIB ainda falta executar num lado do livro.
     *
     * @param side o lado
     * @return a soma de {@code quantity - filled_quantity} das ordens abertas do lado
     */
    long openQuantity(OrderSide side);

    /**
     * O maior id de trade. Os ids vêm de uma identity e nada é apagado, então
     * a variação no tempo é a taxa de trades; não é uma contagem exata, porque
     * um rollback consome ids.
     *
     * @return o maior id, ou zero sem trades
     */
    long lastTradeId();

    /** {@return {@code true} se alguma sessão do Postgres segura o advisory lock do Worker} */
    boolean isWorkerLockHeld();
}
