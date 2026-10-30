package com.meli.orderbook.api.monitoring;

import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.stereotype.Component;

/**
 * O componente {@code worker} de {@code /actuator/health}: UP se algum Worker
 * segura o advisory lock, DOWN se nenhum.
 *
 * <p>Não entra no grupo {@code /actuator/health/api}: com o Worker
 * reiniciando, a API continua aceitando ordens (elas esperam no inbox), e um
 * balanceador não deve tirá-la do ar por isso. O health completo, na raiz,
 * mostra os dois.
 */
@Component
public class WorkerHealthIndicator extends AbstractHealthIndicator {

    private static final String PENDING_COMMANDS_DETAIL = "pendingCommands";

    private final MonitoringService monitoringService;

    public WorkerHealthIndicator(MonitoringService monitoringService) {
        super("Falha ao consultar o estado do Worker");
        this.monitoringService = monitoringService;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        var active = monitoringService.isWorkerActive();
        var pendingCommands = monitoringService.pendingCommands();

        if (active) {
            builder.up();
        } else {
            builder.down();
        }
        builder.withDetail(PENDING_COMMANDS_DETAIL, pendingCommands);
    }
}
