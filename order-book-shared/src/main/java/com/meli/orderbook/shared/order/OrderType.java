package com.meli.orderbook.shared.order;

/**
 * Tipo da ordem. Os valores são os mesmos do CHECK de {@code orders.type}.
 *
 * <ul>
 *   <li>{@code LIMIT}: tem preço limite; o que não executa na hora fica no livro.</li>
 *   <li>{@code MARKET}: sem preço; executa o que der contra o livro e nunca fica nele.</li>
 * </ul>
 */
public enum OrderType {
    LIMIT,
    MARKET
}
