package com.meli.orderbook.worker;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;

class WorkerPropertiesTest {

    @Test
    void should_accept_the_default_configuration() {
        assertThatNoException().isThrownBy(() -> new WorkerProperties(500, Duration.ofMillis(5)));
    }

    @Test
    void should_refuse_a_batch_size_that_would_never_read_anything() {
        assertThatIllegalArgumentException().isThrownBy(() -> new WorkerProperties(0, Duration.ofMillis(5)));
    }

    @Test
    void should_refuse_a_poll_interval_that_would_spin_the_loop() {
        assertThatIllegalArgumentException().isThrownBy(() -> new WorkerProperties(500, Duration.ZERO));
        assertThatIllegalArgumentException().isThrownBy(() -> new WorkerProperties(500, null));
    }
}
