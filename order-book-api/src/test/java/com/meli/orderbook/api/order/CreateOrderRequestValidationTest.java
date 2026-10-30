package com.meli.orderbook.api.order;

import com.meli.orderbook.shared.order.OrderSide;
import com.meli.orderbook.shared.order.OrderType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CreateOrderRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void should_accept_a_valid_limit_order() {
        var request = limitOrder(new BigDecimal("10.50"), 3L);

        assertThat(violationsOf(request)).isEmpty();
    }

    @Test
    void should_accept_a_market_order_when_it_has_no_price() {
        var request = new CreateOrderRequest("alice", "c1", OrderSide.SELL, OrderType.MARKET, null, 3L);

        assertThat(violationsOf(request)).isEmpty();
    }

    @Test
    void should_require_price_when_order_is_limit() {
        var request = limitOrder(null, 3L);

        assertThat(violationsOf(request)).containsExactly("price: O price é obrigatório para ordens LIMIT.");
    }

    @Test
    void should_reject_price_when_order_is_market() {
        var request = new CreateOrderRequest("alice", "c1", OrderSide.BUY, OrderType.MARKET, BigDecimal.TEN, 3L);

        assertThat(violationsOf(request)).containsExactly("price: Ordens MARKET não aceitam price.");
    }

    @Test
    void should_reject_price_when_it_has_more_than_two_decimal_places() {
        var request = limitOrder(new BigDecimal("10.555"), 3L);

        assertThat(violationsOf(request))
                .containsExactly("price: O price deve ter no máximo 15 dígitos inteiros e 2 casas decimais.");
    }

    @Test
    void should_reject_price_when_it_is_not_positive() {
        var request = limitOrder(BigDecimal.ZERO, 3L);

        assertThat(violationsOf(request)).containsExactly("price: O price deve ser maior que zero.");
    }

    @Test
    void should_reject_quantity_when_it_is_not_positive() {
        var request = limitOrder(BigDecimal.TEN, 0L);

        assertThat(violationsOf(request)).containsExactly("quantity: O quantity deve ser maior que zero.");
    }

    @Test
    void should_require_user_and_client_order_id_when_they_are_blank() {
        var request = new CreateOrderRequest("", " ", OrderSide.BUY, OrderType.LIMIT, BigDecimal.TEN, 1L);

        assertThat(violationsOf(request)).containsExactlyInAnyOrder(
                "userId: O userId é obrigatório.",
                "clientOrderId: O clientOrderId é obrigatório.");
    }

    @Test
    void should_reject_user_id_when_it_is_longer_than_100_characters() {
        var longUserId = "u".repeat(101);
        var request = new CreateOrderRequest(longUserId, "c1", OrderSide.BUY, OrderType.LIMIT, BigDecimal.TEN, 1L);

        assertThat(violationsOf(request)).containsExactly("userId: O userId deve ter no máximo 100 caracteres.");
    }

    @Test
    void should_reject_user_id_when_it_contains_spaces() {
        var request = new CreateOrderRequest(" alice", "c1", OrderSide.BUY, OrderType.LIMIT, BigDecimal.TEN, 1L);

        assertThat(violationsOf(request)).containsExactly("userId: O userId não pode conter espaços.");
    }

    @Test
    void should_only_report_missing_type_when_type_is_absent() {
        var request = new CreateOrderRequest("alice", "c1", OrderSide.BUY, null, BigDecimal.TEN, 1L);

        assertThat(violationsOf(request)).containsExactly("type: O type é obrigatório.");
    }

    private static CreateOrderRequest limitOrder(BigDecimal price, Long quantity) {
        return new CreateOrderRequest("alice", "c1", OrderSide.BUY, OrderType.LIMIT, price, quantity);
    }

    private static Set<String> violationsOf(CreateOrderRequest request) {
        return validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.toSet());
    }
}
