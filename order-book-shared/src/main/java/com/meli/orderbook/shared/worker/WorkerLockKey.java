package com.meli.orderbook.shared.worker;

/**
 * A chave do advisory lock do Postgres que garante um único Worker ativo.
 *
 * <p>Fica no {@code shared} porque os dois módulos precisam dela: o Worker
 * segura o lock, e a API consulta {@code pg_locks} para saber se há um Worker
 * ativo. A chave cabe em 32 bits, então o Postgres a guarda com
 * {@code classid = 0} e {@code objid = chave}.
 */
public final class WorkerLockKey {

    /** O valor passado a {@code pg_advisory_lock}. */
    public static final long ADVISORY_LOCK_KEY = 720_261_029L;

    private WorkerLockKey() {
    }
}
