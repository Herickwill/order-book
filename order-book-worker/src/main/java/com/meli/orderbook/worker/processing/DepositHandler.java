package com.meli.orderbook.worker.processing;

import com.meli.orderbook.worker.settlement.Settlement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Aplica {@code DEPOSIT}: credita o disponível e, no primeiro depósito do
 * ativo, cria a carteira. Não toca no livro.
 *
 * <p>Um depósito que estouraria {@code DECIMAL(17,2)} é ignorado com log
 * {@code ERROR}. Sem isso o banco recusaria o lote para sempre: o Worker
 * cairia, subiria, leria o mesmo comando e cairia de novo.
 */
public final class DepositHandler {

    private static final Logger log = LoggerFactory.getLogger(DepositHandler.class);

    private final Settlement settlement;

    /**
     * Cria o handler de {@code DEPOSIT}.
     *
     * @param settlement onde o crédito é feito
     */
    public DepositHandler(Settlement settlement) {
        this.settlement = settlement;
    }

    /**
     * Aplica o comando. A carteira alterada chega ao lote pelo
     * {@code Wallets}, como todas as outras.
     *
     * @param command um comando {@code DEPOSIT}
     */
    public void handle(Command command) {
        var deposited = settlement.deposit(command.userId(), command.asset(), command.amount());
        if (!deposited) {
            log.error("Depósito ignorado: o saldo passaria do limite da coluna. commandId={} userId={} asset={} amount={}",
                    command.id(), command.userId(), command.asset(), command.amount());
        }
    }
}
