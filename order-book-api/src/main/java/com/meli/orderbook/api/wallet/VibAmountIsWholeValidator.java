package com.meli.orderbook.api.wallet;

import com.meli.orderbook.shared.wallet.Asset;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

/** Aplica {@link VibAmountIsWhole} a um {@link CreateDepositRequest}. */
public class VibAmountIsWholeValidator implements ConstraintValidator<VibAmountIsWhole, CreateDepositRequest> {

    private static final String AMOUNT_FIELD = "amount";
    private static final String VIB_MUST_BE_WHOLE = "O amount de um depósito de VIB deve ser um número inteiro.";

    @Override
    public boolean isValid(CreateDepositRequest request, ConstraintValidatorContext context) {
        var isVibDeposit = request.asset() == Asset.VIB;
        var amount = request.amount();

        // Só se aplica a VIB, e um valor ausente já é reportado pelo @NotNull do campo.
        if (!isVibDeposit || amount == null) {
            return true;
        }

        var isWhole = isWholeNumber(amount);
        if (isWhole) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(VIB_MUST_BE_WHOLE)
                .addPropertyNode(AMOUNT_FIELD)
                .addConstraintViolation();
        return false;
    }

    /** {@code 10.00} é inteiro; {@code 2.50} não. Os zeros à direita são descartados antes de olhar as casas. */
    private static boolean isWholeNumber(BigDecimal value) {
        var decimalPlaces = value.stripTrailingZeros().scale();
        return decimalPlaces <= 0;
    }
}
