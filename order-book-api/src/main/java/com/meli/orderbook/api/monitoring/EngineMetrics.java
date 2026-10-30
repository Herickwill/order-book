package com.meli.orderbook.api.monitoring;

import com.meli.orderbook.shared.order.OrderSide;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.stereotype.Component;

/**
 * Registra as gauges do motor em {@code /actuator/prometheus}.
 *
 * <p>Cada gauge é lida do banco no momento do scrape, não acumulada em
 * memória: com duas instâncias da API, ou depois de um restart, o valor
 * continua certo. O custo são algumas consultas pontuais por scrape, todas
 * sobre índices parciais. Se o banco estiver fora do ar, o Micrometer devolve
 * {@code NaN} para a gauge, e o scrape não falha.
 */
@Component
public class EngineMetrics implements MeterBinder {

    static final String INBOX_PENDING = "orderbook.inbox.pending";
    static final String INBOX_OLDEST_PENDING = "orderbook.inbox.oldest.pending";
    static final String BOOK_ORDERS = "orderbook.book.orders";
    static final String BOOK_QUANTITY = "orderbook.book.quantity";
    static final String TRADES_LAST_ID = "orderbook.trades.last.id";
    static final String WORKER_ACTIVE = "orderbook.worker.active";

    private static final String SIDE_TAG = "side";

    private final MonitoringService monitoringService;

    public EngineMetrics(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder(INBOX_PENDING, monitoringService, MonitoringService::pendingCommands)
                .description("Comandos RECEIVED esperando o Worker")
                .register(registry);

        Gauge.builder(INBOX_OLDEST_PENDING, monitoringService, MonitoringService::oldestPendingSeconds)
                .description("Idade do comando pendente mais antigo; zero com a fila vazia")
                .baseUnit("seconds")
                .register(registry);

        for (var side : OrderSide.values()) {
            bindBookSide(registry, side);
        }

        Gauge.builder(TRADES_LAST_ID, monitoringService, MonitoringService::lastTradeId)
                .description("Maior id de trade; rate() dele é a taxa de trades")
                .register(registry);

        Gauge.builder(WORKER_ACTIVE, monitoringService, EngineMetrics::workerActiveAsNumber)
                .description("1 se algum Worker segura o advisory lock, 0 se nenhum")
                .register(registry);
    }

    private void bindBookSide(MeterRegistry registry, OrderSide side) {
        var sideName = side.name();

        Gauge.builder(BOOK_ORDERS, monitoringService, service -> service.openOrders(side))
                .description("Ordens abertas no livro, por lado")
                .tag(SIDE_TAG, sideName)
                .register(registry);

        Gauge.builder(BOOK_QUANTITY, monitoringService, service -> service.openQuantity(side))
                .description("VIB ainda por executar no livro, por lado")
                .tag(SIDE_TAG, sideName)
                .register(registry);
    }

    private static double workerActiveAsNumber(MonitoringService service) {
        var active = service.isWorkerActive();
        if (active) {
            return 1;
        }
        return 0;
    }
}
