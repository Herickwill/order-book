package com.meli.orderbook.api.order;

import com.meli.orderbook.shared.order.OrderType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Aplica {@link PriceMatchesOrderType} a um {@link CreateOrderRequest}. */
public class PriceMatchesOrderTypeValidator implements ConstraintValidator<PriceMatchesOrderType, CreateOrderRequest> {

    private static final String PRICE_FIELD = "price";
    private static final String PRICE_REQUIRED_FOR_LIMIT = "O price é obrigatório para ordens LIMIT.";
    private static final String PRICE_NOT_ALLOWED_FOR_MARKET = "Ordens MARKET não aceitam price.";

    @Override
    public boolean isValid(CreateOrderRequest request, ConstraintValidatorContext context) {
        var type = request.type();

        // Sem tipo não há regra a aplicar; o @NotNull do campo type já reporta a ausência.
        if (type == null) {
            return true;
        }

        var hasPrice = request.price() != null;
        var isLimit = type == OrderType.LIMIT;

        if (isLimit && !hasPrice) {
            reportOnPriceField(context, PRICE_REQUIRED_FOR_LIMIT);
            return false;
        }

        if (!isLimit && hasPrice) {
            reportOnPriceField(context, PRICE_NOT_ALLOWED_FOR_MARKET);
            return false;
        }

        return true;
    }

    /** Troca a mensagem genérica da anotação por uma específica, presa ao campo price. */
    private static void reportOnPriceField(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(PRICE_FIELD)
                .addConstraintViolation();
    }
}
