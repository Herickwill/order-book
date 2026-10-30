package com.meli.orderbook.api.wallet;

import com.meli.orderbook.shared.wallet.Asset;
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

class CreateDepositRequestValidationTest {

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
    void should_accept_brl_deposit_when_it_has_cents() {
        assertThat(violationsOf(deposit(Asset.BRL, "1000.55"))).isEmpty();
    }

    @Test
    void should_accept_vib_deposit_when_it_is_whole() {
        assertThat(violationsOf(deposit(Asset.VIB, "10"))).isEmpty();
    }

    @Test
    void should_accept_vib_deposit_when_it_has_only_zero_decimals() {
        assertThat(violationsOf(deposit(Asset.VIB, "10.00"))).isEmpty();
    }

    @Test
    void should_reject_vib_deposit_when_it_is_fractional() {
        assertThat(violationsOf(deposit(Asset.VIB, "2.5")))
                .containsExactly("amount: O amount de um depósito de VIB deve ser um número inteiro.");
    }

    @Test
    void should_reject_amount_when_it_is_not_positive() {
        assertThat(violationsOf(deposit(Asset.BRL, "0"))).containsExactly("amount: O amount deve ser maior que zero.");
    }

    @Test
    void should_require_amount_when_it_is_absent() {
        var request = new CreateDepositRequest("alice", "dep-1", Asset.BRL, null);

        assertThat(violationsOf(request)).containsExactly("amount: O amount é obrigatório.");
    }

    @Test
    void should_reject_amount_when_it_has_more_than_two_decimal_places() {
        assertThat(violationsOf(deposit(Asset.BRL, "0.005")))
                .containsExactly("amount: O amount deve ter no máximo 15 dígitos inteiros e 2 casas decimais.");
    }

    @Test
    void should_require_asset_when_it_is_absent() {
        var request = new CreateDepositRequest("alice", "dep-1", null, BigDecimal.TEN);

        assertThat(violationsOf(request)).containsExactly("asset: O asset é obrigatório.");
    }

    @Test
    void should_require_deposit_id_when_it_is_blank() {
        var request = new CreateDepositRequest("alice", " ", Asset.BRL, BigDecimal.TEN);

        assertThat(violationsOf(request)).containsExactly("depositId: O depositId é obrigatório.");
    }

    @Test
    void should_require_user_id_when_it_is_absent() {
        var request = new CreateDepositRequest(null, "dep-1", Asset.BRL, BigDecimal.TEN);

        assertThat(violationsOf(request)).containsExactly("userId: O userId é obrigatório.");
    }

    private static CreateDepositRequest deposit(Asset asset, String amount) {
        return new CreateDepositRequest("alice", "dep-1", asset, new BigDecimal(amount));
    }

    private static Set<String> violationsOf(CreateDepositRequest request) {
        return validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.toSet());
    }
}
