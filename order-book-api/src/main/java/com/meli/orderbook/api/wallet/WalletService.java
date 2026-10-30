package com.meli.orderbook.api.wallet;

import com.meli.orderbook.api.common.Page;
import com.meli.orderbook.api.wallet.repository.StatementEntryRow;
import com.meli.orderbook.api.wallet.repository.StatementRepository;
import com.meli.orderbook.api.wallet.repository.WalletRepository;
import com.meli.orderbook.api.wallet.repository.WalletRow;
import com.meli.orderbook.shared.wallet.Asset;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;

/** Leitura dos saldos e do extrato. Quem altera saldo é apenas o Worker. */
@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final StatementRepository statementRepository;

    public WalletService(WalletRepository walletRepository, StatementRepository statementRepository) {
        this.walletRepository = walletRepository;
        this.statementRepository = statementRepository;
    }

    /**
     * As carteiras do usuário, indexadas pelo ativo.
     *
     * @param userId o dono das carteiras
     * @return uma entrada por ativo com linha em {@code wallets}; vazio se o
     *         usuário ainda não tem depósito processado
     */
    public Map<Asset, WalletRow> findByAsset(String userId) {
        var storedWallets = walletRepository.findByUserId(userId);

        var walletsByAsset = new EnumMap<Asset, WalletRow>(Asset.class);
        for (var wallet : storedWallets) {
            walletsByAsset.put(wallet.asset(), wallet);
        }

        return walletsByAsset;
    }

    /**
     * Uma página do extrato do usuário, do mais recente para o mais antigo.
     *
     * @param userId o usuário
     * @param page   a página, começando em zero
     * @param size   o tamanho da página
     * @return a página de depósitos processados e trades
     */
    public Page<StatementEntryRow> findStatementPage(String userId, int page, int size) {
        var offset = Page.offsetOf(page, size);
        var limit = Page.limitWithLookahead(size);
        var entries = statementRepository.findByUser(userId, limit, offset);
        return Page.fromLookahead(entries, page, size);
    }
}
