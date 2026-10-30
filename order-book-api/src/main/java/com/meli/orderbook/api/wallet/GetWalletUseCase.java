package com.meli.orderbook.api.wallet;

import com.meli.orderbook.api.wallet.repository.WalletRow;
import com.meli.orderbook.shared.wallet.Asset;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;

/**
 * Consulta os saldos de um usuário.
 *
 * <p>Um usuário desconhecido recebe todos os ativos zerados, e não 404: a
 * carteira só nasce quando o Worker processa o primeiro depósito, que pode
 * estar na fila.
 */
@Component
public class GetWalletUseCase {

    private final WalletService walletService;

    public GetWalletUseCase(WalletService walletService) {
        this.walletService = walletService;
    }

    /**
     * Monta a carteira com um saldo por ativo, zerando os que não têm linha.
     *
     * @param request o dono da carteira
     * @return os saldos de todos os ativos
     */
    public GetWalletResponse execute(GetWalletRequest request) {
        var userId = request.userId();
        var storedWallets = walletService.findByAsset(userId);

        var balances = Arrays.stream(Asset.values())
                .map(asset -> balanceOf(asset, storedWallets))
                .toList();

        return new GetWalletResponse(userId, balances);
    }

    private static BalanceItem balanceOf(Asset asset, Map<Asset, WalletRow> storedWallets) {
        var storedWallet = storedWallets.get(asset);

        if (storedWallet == null) {
            return BalanceItem.empty(asset);
        }

        return BalanceItem.from(storedWallet);
    }
}
