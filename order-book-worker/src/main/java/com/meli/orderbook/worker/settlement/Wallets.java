package com.meli.orderbook.worker.settlement;

import com.meli.orderbook.shared.wallet.Asset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Todas as carteiras em memória, por usuário e ativo.
 *
 * <p>Também anota quais carteiras mudaram desde a última consulta: são elas que
 * o lote grava. O {@link Settlement} marca cada carteira que altera, e o
 * {@code CommandProcessor} recolhe a lista com {@link #drainTouched()} no fim
 * do lote.
 */
public final class Wallets {

    private final Map<WalletKey, Wallet> wallets = new HashMap<>();
    private final Map<WalletKey, Wallet> touched = new LinkedHashMap<>();

    /**
     * A carteira, se existir. Não cria: um usuário sem carteira tem saldo zero,
     * e uma ordem rejeitada não deve criar carteira.
     *
     * @param userId o dono
     * @param asset  o ativo
     * @return a carteira, ou vazio
     */
    public Optional<Wallet> find(String userId, Asset asset) {
        var key = new WalletKey(userId, asset);
        var wallet = wallets.get(key);
        return Optional.ofNullable(wallet);
    }

    /**
     * Adiciona uma carteira lida do banco na reconstrução do estado. Não a marca
     * como alterada.
     *
     * @param wallet a carteira restaurada
     * @throws IllegalStateException se já existe carteira para a mesma chave
     */
    public void restore(Wallet wallet) {
        var previous = wallets.putIfAbsent(wallet.key(), wallet);
        if (previous != null) {
            throw new IllegalStateException("Carteira restaurada duas vezes. %s".formatted(wallet));
        }
    }

    /**
     * As carteiras alteradas desde a última chamada, na ordem da primeira
     * alteração. Esvazia a anotação.
     *
     * @return as carteiras alteradas, com o estado atual
     */
    public List<Wallet> drainTouched() {
        var result = new ArrayList<>(touched.values());
        touched.clear();
        return result;
    }

    /** {@return todas as carteiras} */
    public List<Wallet> all() {
        return List.copyOf(wallets.values());
    }

    /** A carteira, criada zerada se ainda não existe. Usado em quem recebe valor. */
    Wallet getOrCreate(String userId, Asset asset) {
        var key = new WalletKey(userId, asset);
        return wallets.computeIfAbsent(key, Wallet::empty);
    }

    /** A carteira que tem de existir porque há reserva nela; se não existe, é bug. */
    Wallet require(String userId, Asset asset) {
        var wallet = find(userId, asset);
        return wallet.orElseThrow(() -> new IllegalStateException(
                "Carteira ausente onde deveria haver reserva. userId=%s asset=%s".formatted(userId, asset)));
    }

    /** Anota que a carteira mudou e precisa ir para o lote. */
    void markTouched(Wallet wallet) {
        touched.putIfAbsent(wallet.key(), wallet);
    }
}
