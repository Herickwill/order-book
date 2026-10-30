package com.meli.orderbook.api.common;

import com.meli.orderbook.shared.command.CommandType;
import com.meli.orderbook.shared.order.OrderSide;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InvalidValueMessageTest {

    @Test
    void should_join_two_enum_values_with_ou_when_the_field_is_an_enum() {
        assertThat(InvalidValueMessage.of("side", OrderSide.class)).isEqualTo("O side deve ser BUY ou SELL.");
    }

    @Test
    void should_separate_with_commas_when_the_enum_has_three_values() {
        assertThat(InvalidValueMessage.of("type", CommandType.class))
                .isEqualTo("O type deve ser NEW_ORDER, CANCEL_ORDER ou DEPOSIT.");
    }

    @Test
    void should_ask_for_an_integer_when_the_field_is_integral() {
        assertThat(InvalidValueMessage.of("quantity", Long.class)).isEqualTo("O quantity deve ser um número inteiro.");
        assertThat(InvalidValueMessage.of("page", Integer.class)).isEqualTo("O page deve ser um número inteiro.");
    }

    @Test
    void should_ask_for_a_number_when_the_field_is_decimal() {
        assertThat(InvalidValueMessage.of("amount", BigDecimal.class)).isEqualTo("O amount deve ser um número.");
    }

    @Test
    void should_ask_for_a_uuid_when_the_field_is_a_uuid() {
        assertThat(InvalidValueMessage.of("orderId", UUID.class)).isEqualTo("O orderId deve ser um UUID válido.");
    }

    @Test
    void should_fall_back_to_a_generic_message_when_the_type_is_unknown() {
        assertThat(InvalidValueMessage.of("x", OffsetDateTime.class)).isEqualTo("O x tem um valor inválido.");
        assertThat(InvalidValueMessage.of("x", null)).isEqualTo("O x tem um valor inválido.");
    }
}
