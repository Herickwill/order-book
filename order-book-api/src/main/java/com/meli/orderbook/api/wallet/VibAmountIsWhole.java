package com.meli.orderbook.api.wallet;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Exige que depósitos de VIB sejam em unidades inteiras.
 *
 * <p>É uma regra entre dois campos ({@code asset} e {@code amount}), por isso
 * fica na classe. O erro é reportado no campo {@code amount}.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = VibAmountIsWholeValidator.class)
public @interface VibAmountIsWhole {

    /**
     * Substituída pela mensagem presa ao campo {@code amount} no validador.
     *
     * @return a mensagem padrão
     */
    String message() default "O amount de um depósito de VIB deve ser um número inteiro.";

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
