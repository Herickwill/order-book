package com.meli.orderbook.api.wallet;

import com.meli.orderbook.api.common.ValidUserId;
import com.meli.orderbook.shared.wallet.Asset;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Entrada de {@link CreateDepositUseCase}, lida do corpo de {@code POST /deposits}.
 *
 * @param userId    o dono da carteira; a carteira nasce no primeiro depósito processado
 * @param depositId chave de idempotência gerada pelo cliente
 * @param asset     o ativo depositado
 * @param amount    o valor; inteiro quando o ativo é VIB
 */
@VibAmountIsWhole
public record CreateDepositRequest(

        @ValidUserId
        String userId,

        @NotBlank(message = "O depositId é obrigatório.")
        @Size(max = 150, message = "O depositId deve ter no máximo 150 caracteres.")
        String depositId,

        @NotNull(message = "O asset é obrigatório.")
        Asset asset,

        // Sem o limite de casas, o Postgres arredondaria o valor em silêncio (0.005 -> 0.01).
        @NotNull(message = "O amount é obrigatório.")
        @Positive(message = "O amount deve ser maior que zero.")
        @Digits(integer = 15, fraction = 2,
                message = "O amount deve ter no máximo 15 dígitos inteiros e 2 casas decimais.")
        BigDecimal amount
) {
}
