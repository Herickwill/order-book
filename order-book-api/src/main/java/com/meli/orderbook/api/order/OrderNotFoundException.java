package com.meli.orderbook.api.order;

import java.io.Serial;
import java.util.UUID;

/** Nenhuma ordem com este id, nem processada nem aguardando no inbox. */
public class OrderNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Cria a exceção para o id procurado.
     *
     * @param orderId o id que não foi encontrado
     */
    public OrderNotFoundException(UUID orderId) {
        super("Ordem não encontrada: " + orderId);
    }
}
