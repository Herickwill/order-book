package com.meli.orderbook.api.common;

/**
 * Um erro de validação, como aparece na lista {@code errors} do Problem Detail.
 *
 * @param field   o campo afetado, pelo nome do JSON ou do parâmetro
 * @param message uma frase explicando a regra violada
 */
public record ValidationError(String field, String message) {
}
