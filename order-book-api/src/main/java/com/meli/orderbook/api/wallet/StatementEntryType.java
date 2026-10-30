package com.meli.orderbook.api.wallet;

/** O que gerou uma linha do extrato. */
public enum StatementEntryType {
    /** Depósito processado: o ativo depositado aumenta. */
    DEPOSIT,
    /** Compra executada: sai BRL, entra VIB. */
    BUY,
    /** Venda executada: sai VIB, entra BRL. */
    SELL
}
