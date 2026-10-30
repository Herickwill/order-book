package com.meli.orderbook.api.wallet;

import org.springframework.stereotype.Component;

/**
 * Consulta o extrato de um usuário: depósitos processados e trades, com o
 * efeito de cada um no saldo.
 *
 * <p>É a rastreabilidade do saldo: somando as variações do extrato, chega-se
 * ao total de cada ativo da carteira. Um usuário sem histórico recebe uma
 * página vazia, e não 404, pelo mesmo motivo de {@link GetWalletUseCase}.
 */
@Component
public class GetStatementUseCase {

    private final WalletService walletService;

    public GetStatementUseCase(WalletService walletService) {
        this.walletService = walletService;
    }

    /**
     * Busca a página pedida e a converte em resposta.
     *
     * @param request o dono da carteira e a paginação
     * @return a página do extrato
     */
    public GetStatementResponse execute(GetStatementRequest request) {
        var userId = request.userId();
        var entries = walletService.findStatementPage(userId, request.page(), request.size());
        var items = entries.map(StatementEntryItem::from);
        return GetStatementResponse.from(userId, items);
    }
}
