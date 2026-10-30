package com.meli.orderbook.shared.order;

/**
 * Estado de uma ordem já processada pelo Worker. Os valores são os mesmos do
 * CHECK de {@code orders.status}.
 *
 * <p>Ciclo de vida: {@code NEW} → {@code PARTIALLY_FILLED} → {@code FILLED}.
 * Uma ordem aberta pode virar {@code CANCELLED}, e {@code REJECTED} só
 * acontece na chegada, quando a ordem não executou nada.
 *
 * <p>{@code FILLED}, {@code CANCELLED} e {@code REJECTED} são finais. Quem
 * valida as transições é a classe {@code Order} do Worker; este enum só diz se
 * o estado está aberto ou não.
 */
public enum OrderStatus {
    NEW,
    PARTIALLY_FILLED,
    FILLED,
    CANCELLED,
    REJECTED;

    /**
     * Se a ordem ainda pode executar: está no livro ou acabou de chegar.
     *
     * @return {@code true} para {@code NEW} e {@code PARTIALLY_FILLED}
     */
    public boolean isOpen() {
        return switch (this) {
            case NEW, PARTIALLY_FILLED -> true;
            case FILLED, CANCELLED, REJECTED -> false;
        };
    }
}
