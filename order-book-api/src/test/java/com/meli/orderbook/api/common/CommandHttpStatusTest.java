package com.meli.orderbook.api.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class CommandHttpStatusTest {

    @Test
    void should_answer_accepted_when_the_command_is_new() {
        assertThat(CommandHttpStatus.of(true)).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    void should_answer_ok_when_the_command_is_a_resend() {
        assertThat(CommandHttpStatus.of(false)).isEqualTo(HttpStatus.OK);
    }
}
