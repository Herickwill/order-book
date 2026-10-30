package com.meli.orderbook.worker.persistence;

import java.io.Serial;

/**
 * A conexão do Worker não é mais a dona do advisory lock.
 *
 * <p>Acontece quando o pool trocou a conexão (queda de rede, por exemplo) ou
 * alguém derrubou a sessão no Postgres. Outro Worker pode já ter assumido, e
 * gravar seria sobrescrever o estado dele com uma memória velha. O lote é
 * desfeito e o processo encerra.
 */
public class LockLostException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Cria a exceção.
     *
     * @param message o que foi conferido
     */
    public LockLostException(String message) {
        super(message);
    }
}
