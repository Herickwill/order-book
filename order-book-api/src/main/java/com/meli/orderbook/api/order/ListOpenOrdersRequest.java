package com.meli.orderbook.api.order;

import com.meli.orderbook.api.common.Page;
import com.meli.orderbook.shared.order.OrderSide;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Entrada de {@link ListOpenOrdersUseCase}, montada a partir da query de
 * {@code GET /orders?side=&userId=&page=&size=}.
 *
 * @param side   o lado do livro a listar
 * @param userId filtra por usuário; ausente ou vazio lista todos
 * @param page   a página, começando em zero; {@value Page#FIRST_PAGE} se ausente
 * @param size   o tamanho da página; {@value Page#DEFAULT_SIZE} se ausente
 */
public record ListOpenOrdersRequest(

        @NotNull(message = "O side é obrigatório.")
        OrderSide side,

        @Size(max = 100, message = "O userId deve ter no máximo 100 caracteres.")
        String userId,

        @Min(value = Page.FIRST_PAGE, message = "O page deve ser maior ou igual a zero.")
        Integer page,

        @Min(value = 1, message = "O size deve estar entre 1 e 100.")
        @Max(value = Page.MAX_SIZE, message = "O size deve estar entre 1 e 100.")
        Integer size
) {

    /**
     * Aplica os valores padrão. {@code ?userId=} vazio significa "sem filtro",
     * e não "usuário de nome vazio".
     */
    public ListOpenOrdersRequest {
        if (userId != null && userId.isBlank()) {
            userId = null;
        }

        if (page == null) {
            page = Page.FIRST_PAGE;
        }

        if (size == null) {
            size = Page.DEFAULT_SIZE;
        }
    }
}
