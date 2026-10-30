package com.meli.orderbook.api.order;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Exige que o preço seja coerente com o tipo da ordem: {@code LIMIT} precisa
 * de preço, {@code MARKET} não aceita preço.
 *
 * <p>É uma regra entre dois campos, por isso fica na classe. O erro, porém, é
 * reportado no campo {@code price}, que é onde o cliente precisa corrigir.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PriceMatchesOrderTypeValidator.class)
public @interface PriceMatchesOrderType {

    /**
     * Substituída pela mensagem específica de cada caso no validador.
     *
     * @return a mensagem padrão
     */
    String message() default "O price não é compatível com o type da ordem.";

    /**
     * Grupos de validação; não usados no projeto.
     *
     * @return os grupos
     */
    Class<?>[] groups() default {};

    /**
     * Metadados da violação; não usados no projeto.
     *
     * @return o payload
     */
    Class<? extends Payload>[] payload() default {};
}
