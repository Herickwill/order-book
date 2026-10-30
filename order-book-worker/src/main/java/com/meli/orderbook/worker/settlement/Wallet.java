package com.meli.orderbook.worker.settlement;

import com.meli.orderbook.shared.wallet.Asset;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * O saldo de um usuário num ativo: {@code available} pode ser usado,
 * {@code reserved} está preso a ordens abertas.
 *
 * <p>Toda operação recebe um valor positivo e falha com
 * {@link IllegalStateException} se deixaria algum saldo negativo. Isso nunca
 * acontece por decisão de negócio (quem decide checa antes com
 * {@link #covers}); se acontece, é bug, e o processo encerra.
 */
public final class Wallet {

    /** O maior valor que cabe em {@code DECIMAL(17,2)}. */
    static final BigDecimal COLUMN_LIMIT = new BigDecimal("999999999999999.99");

    private final WalletKey key;
    private BigDecimal available;
    private BigDecimal reserved;

    private Wallet(WalletKey key, BigDecimal available, BigDecimal reserved) {
        this.key = Objects.requireNonNull(key, "key");
        this.available = Objects.requireNonNull(available, "available");
        this.reserved = Objects.requireNonNull(reserved, "reserved");

        var negative = available.signum() < 0 || reserved.signum() < 0;
        if (negative) {
            throw new IllegalArgumentException("Carteira com saldo negativo. %s".formatted(this));
        }
    }

    /**
     * Uma carteira nova, zerada.
     *
     * @param key o dono e o ativo
     * @return a carteira
     */
    public static Wallet empty(WalletKey key) {
        return new Wallet(key, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    /**
     * Uma carteira lida do banco na reconstrução do estado.
     *
     * @param key       o dono e o ativo
     * @param available o saldo disponível gravado
     * @param reserved  o saldo reservado gravado
     * @return a carteira
     */
    public static Wallet restore(WalletKey key, BigDecimal available, BigDecimal reserved) {
        return new Wallet(key, available, reserved);
    }

    /**
     * Se o disponível cobre o valor.
     *
     * @param amount o valor a cobrir
     * @return {@code true} se {@code available >= amount}
     */
    public boolean covers(BigDecimal amount) {
        return available.compareTo(amount) >= 0;
    }

    /**
     * Se um depósito ainda cabe na coluna. Checar o total
     * ({@code available + reserved}) garante que as duas colunas cabem.
     *
     * <p>Limitação assumida: só o depósito é checado. Um vendedor que recebe BRL
     * num trade poderia, em tese, passar do limite; para isso o sistema
     * precisaria ter mais de R$ 10<sup>15</sup> numa única carteira.
     *
     * @param amount o valor a creditar
     * @return {@code true} se o total depois do crédito não passa do limite
     */
    public boolean canCredit(BigDecimal amount) {
        var totalAfter = total().add(amount);
        return totalAfter.compareTo(COLUMN_LIMIT) <= 0;
    }

    /** {@return {@code available + reserved}} */
    public BigDecimal total() {
        return available.add(reserved);
    }

    /** Move {@code amount} do disponível para o reservado. */
    void reserve(BigDecimal amount) {
        requirePositive(amount);
        var covered = covers(amount);
        if (!covered) {
            throw new IllegalStateException("Reserva acima do disponível. amount=%s %s".formatted(amount, this));
        }

        available = available.subtract(amount);
        reserved = reserved.add(amount);
    }

    /** Devolve {@code amount} do reservado para o disponível. */
    void release(BigDecimal amount) {
        requirePositive(amount);
        requireReserved(amount);

        reserved = reserved.subtract(amount);
        available = available.add(amount);
    }

    /** Tira {@code amount} do reservado: é o valor pago num trade. */
    void spendReserved(BigDecimal amount) {
        requirePositive(amount);
        requireReserved(amount);

        reserved = reserved.subtract(amount);
    }

    /** Soma {@code amount} ao disponível: depósito ou valor recebido num trade. */
    void credit(BigDecimal amount) {
        requirePositive(amount);
        available = available.add(amount);
    }

    /** {@return o dono e o ativo} */
    public WalletKey key() {
        return key;
    }

    /** {@return o dono} */
    public String userId() {
        return key.userId();
    }

    /** {@return o ativo} */
    public Asset asset() {
        return key.asset();
    }

    /** {@return o saldo disponível} */
    public BigDecimal available() {
        return available;
    }

    /** {@return o saldo preso a ordens abertas} */
    public BigDecimal reserved() {
        return reserved;
    }

    @Override
    public String toString() {
        return "Wallet[userId=%s, asset=%s, available=%s, reserved=%s]"
                .formatted(key.userId(), key.asset(), available, reserved);
    }

    private void requirePositive(BigDecimal amount) {
        var positive = amount.signum() > 0;
        if (!positive) {
            throw new IllegalStateException("Valor não positivo. amount=%s %s".formatted(amount, this));
        }
    }

    private void requireReserved(BigDecimal amount) {
        var enough = reserved.compareTo(amount) >= 0;
        if (!enough) {
            throw new IllegalStateException("Valor acima do reservado. amount=%s %s".formatted(amount, this));
        }
    }
}
