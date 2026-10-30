package com.meli.orderbook.api.monitoring;

import com.meli.orderbook.shared.order.OrderSide;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EngineMetricsTest {

    @Mock
    private MonitoringService monitoringService;

    private SimpleMeterRegistry registry;

    @BeforeEach
    void bind() {
        registry = new SimpleMeterRegistry();
        new EngineMetrics(monitoringService).bindTo(registry);
    }

    @Test
    void should_read_the_queue_from_the_service_on_every_scrape() {
        when(monitoringService.pendingCommands()).thenReturn(7L, 0L);

        var gauge = registry.get(EngineMetrics.INBOX_PENDING).gauge();

        assertThat(gauge.value()).isEqualTo(7.0);
        assertThat(gauge.value()).isZero();
    }

    @Test
    void should_report_each_book_side_under_its_own_tag() {
        when(monitoringService.openOrders(OrderSide.BUY)).thenReturn(4L);
        when(monitoringService.openOrders(OrderSide.SELL)).thenReturn(9L);

        var buy = registry.get(EngineMetrics.BOOK_ORDERS).tag("side", "BUY").gauge();
        var sell = registry.get(EngineMetrics.BOOK_ORDERS).tag("side", "SELL").gauge();

        assertThat(buy.value()).isEqualTo(4.0);
        assertThat(sell.value()).isEqualTo(9.0);
    }

    @Test
    void should_report_one_when_a_worker_is_active_and_zero_when_none_is() {
        when(monitoringService.isWorkerActive()).thenReturn(true, false);

        var gauge = registry.get(EngineMetrics.WORKER_ACTIVE).gauge();

        assertThat(gauge.value()).isEqualTo(1.0);
        assertThat(gauge.value()).isZero();
    }

    @Test
    void should_register_every_engine_gauge() {
        assertThat(registry.find(EngineMetrics.INBOX_OLDEST_PENDING).gauge()).isNotNull();
        assertThat(registry.find(EngineMetrics.BOOK_QUANTITY).gauges()).hasSize(2);
        assertThat(registry.find(EngineMetrics.TRADES_LAST_ID).gauge()).isNotNull();
    }
}
