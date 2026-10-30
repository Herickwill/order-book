package com.meli.orderbook.api.common;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

/**
 * Mensagem para um valor que não pôde ser convertido para o tipo do campo,
 * como {@code "side": "COMPRA"} ou {@code ?size=abc}.
 *
 * <p>A frase é derivada do tipo esperado, e não do nome do campo: assim o
 * tratamento de erros não precisa conhecer os campos de cada feature, e um
 * enum novo já ganha a mensagem com os seus valores.
 */
final class InvalidValueMessage {

    private static final Set<Class<?>> INTEGER_TYPES =
            Set.of(Long.class, long.class, Integer.class, int.class, Short.class, short.class);

    private static final Set<Class<?>> DECIMAL_TYPES =
            Set.of(BigDecimal.class, Double.class, double.class, Float.class, float.class);

    private InvalidValueMessage() {
    }

    /**
     * A frase explicando o que o campo aceita.
     *
     * @param field        o nome do campo, como o cliente o enviou
     * @param expectedType o tipo para o qual a conversão falhou; pode ser nulo
     * @return por exemplo {@code "O side deve ser BUY ou SELL."}
     */
    static String of(String field, Class<?> expectedType) {
        var expectation = expectationFor(expectedType);
        return "O " + field + " " + expectation + ".";
    }

    private static String expectationFor(Class<?> expectedType) {
        if (expectedType == null) {
            return "tem um valor inválido";
        }

        if (expectedType.isEnum()) {
            return "deve ser " + acceptedValuesOf(expectedType);
        }

        if (INTEGER_TYPES.contains(expectedType)) {
            return "deve ser um número inteiro";
        }

        if (DECIMAL_TYPES.contains(expectedType)) {
            return "deve ser um número";
        }

        if (expectedType == UUID.class) {
            return "deve ser um UUID válido";
        }

        return "tem um valor inválido";
    }

    /** {@code BUY ou SELL}; com três ou mais valores, {@code A, B ou C}. */
    private static String acceptedValuesOf(Class<?> enumType) {
        var names = Arrays.stream(enumType.getEnumConstants())
                .map(constant -> ((Enum<?>) constant).name())
                .toList();

        var lastIndex = names.size() - 1;
        if (lastIndex == 0) {
            return names.getFirst();
        }

        var allButLast = String.join(", ", names.subList(0, lastIndex));
        return allButLast + " ou " + names.get(lastIndex);
    }
}
