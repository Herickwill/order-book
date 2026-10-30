package com.meli.orderbook.shared.command;

/** Tipo de comando no inbox. Os valores são os mesmos do CHECK de {@code commands.type}. */
public enum CommandType {
    NEW_ORDER,
    CANCEL_ORDER,
    DEPOSIT
}
