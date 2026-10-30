package com.meli.orderbook.api.monitoring;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkerHealthIndicatorTest {

    @Mock
    private MonitoringService monitoringService;

    @InjectMocks
    private WorkerHealthIndicator workerHealthIndicator;

    @Test
    void should_be_up_and_show_the_queue_when_a_worker_holds_the_lock() {
        when(monitoringService.isWorkerActive()).thenReturn(true);
        when(monitoringService.pendingCommands()).thenReturn(3L);

        var health = workerHealthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("pendingCommands", 3L);
    }

    @Test
    void should_be_down_when_no_worker_holds_the_lock() {
        when(monitoringService.isWorkerActive()).thenReturn(false);
        when(monitoringService.pendingCommands()).thenReturn(120L);

        var health = workerHealthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }

    @Test
    void should_be_down_when_the_database_cannot_be_queried() {
        when(monitoringService.isWorkerActive()).thenThrow(new IllegalStateException("banco fora"));

        var health = workerHealthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }
}
