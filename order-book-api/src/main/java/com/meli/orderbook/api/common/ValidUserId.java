package com.meli.orderbook.api.common;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;

/**
 * Regras de um {@code userId} obrigatório: presente, com até 100 caracteres
 * (o tamanho da coluna) e sem espaços. Sem a regra dos espaços,
 * {@code " alice"} e {@code "alice"} seriam usuários diferentes.
 *
 * <p>Cada regra reporta a sua própria mensagem.
 */
@Documented
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@NotBlank(message = "O userId é obrigatório.")
@Size(max = 100, message = "O userId deve ter no máximo 100 caracteres.")
@Pattern(regexp = "\\S*", message = "O userId não pode conter espaços.")
public @interface ValidUserId {

    /**
     * Não é usada: cada regra composta tem a sua mensagem.
     *
     * @return a mensagem padrão
     */
    String message() default "O userId é inválido.";

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
