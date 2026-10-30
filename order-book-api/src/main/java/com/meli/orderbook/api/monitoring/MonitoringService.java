package com.meli.orderbook.api.monitoring;

import com.meli.orderbook.api.monitoring.repository.MonitoringRepository;
import com.meli.orderbook.shared.order.OrderSide;
import org.springframework.stereotype.Service;

/**
 * O estado do motor visto pelo banco, para métricas e health.
 *
 * <p>O Worker não tem servidor web, então as métricas dele são medidas onde
 * ele deixa rastro: na fila de {@code commands}, nas ordens abertas, nos trades
 * e no advisory lock. O banco é a verdade do sistema, então o que se mede aqui
 * é o que de fato aconteceu, e não o que o processo acha que fez.
 */
@Service
public class MonitoringService {

    private final MonitoringRepository monitoringRepository;

    public MonitoringService(MonitoringRepository monitoringRepository) {
        this.monitoringRepository = monitoringRepository;
    }

    /** {@return quantos comandos {@code RECEIVED} esperam o Worker} */
    public long pendingCommands() {
        return monitoringRepository.countPendingCommands();
    }

    /** {@return a idade, em segundos, do comando pendente mais antigo; zero com a fila vazia} */
    public double oldestPendingSeconds() {
        return monitoringRepository.oldestPendingSeconds();
    }

    /**
     * Quantas ordens abertas há num lado do livro.
     *
     * @param side o lado
     * @return a contagem
     */
    public long openOrders(OrderSide side) {
        return monitoringRepository.countOpenOrders(side);
    }

    /**
     * Quanto VIB falta executar num lado do livro.
     *
     * @param side o lado
     * @return a soma do restante das ordens abertas
     */
    public long openQuantity(OrderSide side) {
        return monitoringRepository.openQuantity(side);
    }

    /** {@return o maior id de trade; a variação dele no tempo é a taxa de trades} */
    public long lastTradeId() {
        return monitoringRepository.lastTradeId();
    }

    /** {@return {@code true} se há um Worker ativo, isto é, alguém segura o advisory lock} */
    public boolean isWorkerActive() {
        return monitoringRepository.isWorkerLockHeld();
    }
}
