package com.meli.orderbook.api.trade;

import com.meli.orderbook.api.common.Page;
import com.meli.orderbook.api.common.ValidUserId;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Entrada de {@link ListTradesUseCase}, montada a partir da query de
 * {@code GET /trades?userId=&page=&size=}.
 *
 * @param userId o usuário cujo histórico será listado
 * @param page   a página, começando em zero; {@value Page#FIRST_PAGE} se ausente
 * @param size   o tamanho da página; {@value Page#DEFAULT_SIZE} se ausente
 */
public record ListTradesRequest(

        @ValidUserId
        String userId,

        @Min(value = Page.FIRST_PAGE, message = "O page deve ser maior ou igual a zero.")
        Integer page,

        @Min(value = 1, message = "O size deve estar entre 1 e 100.")
        @Max(value = Page.MAX_SIZE, message = "O size deve estar entre 1 e 100.")
        Integer size
) {

    /** Aplica os valores padrão de paginação. */
    public ListTradesRequest {
        if (page == null) {
            page = Page.FIRST_PAGE;
        }

        if (size == null) {
            size = Page.DEFAULT_SIZE;
        }
    }
}
