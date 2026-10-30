package com.meli.orderbook.worker.persistence;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import static com.meli.orderbook.shared.worker.WorkerLockKey.ADVISORY_LOCK_KEY;

/**
 * Garante um único Worker ativo com um advisory lock de sessão do Postgres.
 *
 * <p>O lock pertence à conexão, e não ao processo. Por isso:
 * <ul>
 *   <li>o Worker usa uma única conexão a vida inteira (Hikari com
 *       {@code maximum-pool-size=1} e {@code max-lifetime=0});</li>
 *   <li>cada lote confere, na própria transação, que a sessão atual ainda é a
 *       dona. Se o pool trocou a conexão, a sessão nova não tem o lock, a
 *       conferência falha e o processo encerra antes de gravar.</li>
 * </ul>
 *
 * <p>Quando o processo morre, a conexão cai e o Postgres solta o lock sozinho.
 */
@Component
public class WorkerLock {

    private static final String ACQUIRE = """
            SELECT pg_advisory_lock(:key)
            """;

    /**
     * Procura o lock em {@code pg_locks}. Uma chave de 64 bits aparece com os
     * 32 bits altos em {@code classid} (zero, porque a chave é pequena), os
     * baixos em {@code objid} e {@code objsubid = 1}; {@code pid} é o processo
     * do Postgres que atende esta conexão.
     */
    private static final String HELD_BY_THIS_SESSION = """
            SELECT EXISTS (
                SELECT 1
                FROM pg_locks
                WHERE locktype = 'advisory'
                  AND classid = 0
                  AND objid = :key
                  AND objsubid = 1
                  AND granted
                  AND pid = pg_backend_pid())
            """;

    private final JdbcClient jdbcClient;

    public WorkerLock(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /**
     * Espera até conseguir o lock. Um segundo Worker fica parado aqui até a
     * sessão do primeiro terminar, e então assume.
     */
    public void acquire() {
        jdbcClient.sql(ACQUIRE)
                .param("key", ADVISORY_LOCK_KEY)
                .query()
                .listOfRows();
    }

    /**
     * Confere que a sessão atual é a dona do lock. Chamado no início de cada
     * transação de lote, na mesma conexão que fará o commit, então não há
     * janela entre a conferência e a gravação.
     *
     * @throws LockLostException se a sessão não tem o lock
     */
    public void requireHeld() {
        var held = jdbcClient.sql(HELD_BY_THIS_SESSION)
                .param("key", ADVISORY_LOCK_KEY)
                .query(Boolean.class)
                .single();

        if (!held) {
            throw new LockLostException(
                    "A sessão atual não tem o advisory lock do Worker. key=%d".formatted(ADVISORY_LOCK_KEY));
        }
    }
}
