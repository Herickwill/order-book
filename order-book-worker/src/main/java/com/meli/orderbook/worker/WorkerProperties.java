package com.meli.orderbook.worker;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuração do loop, em {@code worker.*}.
 *
 * <p>{@code batchSize} é o máximo de comandos por lote, não um tamanho fixo:
 * com pouca fila o Worker leva o que houver. Sob carga os lotes enchem, e o
 * custo de I/O por comando cai.
 *
 * @param batchSize    o máximo de comandos lidos e gravados por transação
 * @param pollInterval quanto esperar antes de ler de novo quando a fila está vazia
 */
@ConfigurationProperties(prefix = "worker")
public record WorkerProperties(int batchSize, Duration pollInterval) {

    /**
     * Rejeita configuração que travaria ou giraria o loop em falso.
     */
    public WorkerProperties {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("worker.batch-size deve ser positivo.");
        }
        if (pollInterval == null || pollInterval.isNegative() || pollInterval.isZero()) {
            throw new IllegalArgumentException("worker.poll-interval deve ser positivo.");
        }
    }
}
