package com.meli.orderbook.api.wallet;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GetStatementRequestTest {

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
    void should_use_the_first_page_and_the_default_size_when_they_are_absent() {
        var request = new GetStatementRequest("alice", null, null);

        assertThat(request.page()).isZero();
        assertThat(request.size()).isEqualTo(20);
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void should_reject_a_size_above_the_maximum() {
        var violations = validator.validate(new GetStatementRequest("alice", 0, 101));

        assertThat(violations).singleElement()
                .satisfies(violation -> assertThat(violation.getMessage()).isEqualTo("O size deve estar entre 1 e 100."));
    }

    @Test
    void should_reject_a_negative_page() {
        var violations = validator.validate(new GetStatementRequest("alice", -1, 20));

        assertThat(violations).singleElement()
                .satisfies(violation -> assertThat(violation.getPropertyPath()).hasToString("page"));
    }
}
