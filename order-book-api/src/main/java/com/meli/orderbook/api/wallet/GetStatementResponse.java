package com.meli.orderbook.api.wallet;

import com.meli.orderbook.api.common.Page;

import java.util.List;

/**
 * Saída de {@link GetStatementUseCase}: uma página do extrato, do mais recente
 * para o mais antigo.
 *
 * @param userId  o dono da carteira
 * @param items   as linhas da página
 * @param page    o número da página
 * @param size    o tamanho de página pedido
 * @param hasNext {@code true} se existe próxima página
 */
public record GetStatementResponse(
        String userId,
        List<StatementEntryItem> items,
        int page,
        int size,
        boolean hasNext
) {

    /**
     * Monta a resposta a partir de uma página já convertida.
     *
     * @param userId  o dono da carteira
     * @param entries a página do extrato
     * @return a resposta
     */
    public static GetStatementResponse from(String userId, Page<StatementEntryItem> entries) {
        return new GetStatementResponse(userId, entries.items(), entries.page(), entries.size(), entries.hasNext());
    }
}
