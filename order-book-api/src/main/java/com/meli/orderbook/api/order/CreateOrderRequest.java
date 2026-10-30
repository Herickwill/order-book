package com.meli.orderbook.api.order;

import com.meli.orderbook.api.common.ValidUserId;
import com.meli.orderbook.shared.order.OrderSide;
import com.meli.orderbook.shared.order.OrderType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Entrada de {@link CreateOrderUseCase}, lida do corpo de {@code POST /orders}.
 *
 * <p>Só valida o formato. Saldo não é validado aqui: quem decide é o Worker.
 *
 * @param userId        quem envia a ordem
 * @param clientOrderId chave de idempotência gerada pelo cliente: um reenvio
 *                      com a mesma chave devolve a ordem original
 * @param side          compra ou venda
 * @param type          {@code LIMIT} ou {@code MARKET}
 * @param price         preço limite; obrigatório em {@code LIMIT}, proibido em {@code MARKET}
 * @param quantity      quantidade de VIB, em unidades inteiras
 */
@PriceMatchesOrderType
public record CreateOrderRequest(

        @ValidUserId
        String userId,

        @NotBlank(message = "O clientOrderId é obrigatório.")
        @Size(max = 150, message = "O clientOrderId deve ter no máximo 150 caracteres.")
        String clientOrderId,

        @NotNull(message = "O side é obrigatório.")
        OrderSide side,

        @NotNull(message = "O type é obrigatório.")
        OrderType type,

        @Positive(message = "O price deve ser maior que zero.")
        @Digits(integer = 15, fraction = 2,
                message = "O price deve ter no máximo 15 dígitos inteiros e 2 casas decimais.")
        BigDecimal price,

        @NotNull(message = "O quantity é obrigatório.")
        @Positive(message = "O quantity deve ser maior que zero.")
        Long quantity
) {
}
