package com.meli.orderbook.api.common;

import org.springframework.http.HttpStatus;

/**
 * O status HTTP de um comando registrado no inbox. É a única regra de status
 * da API, e fica num lugar só para que os endpoints de escrita não divirjam.
 */
public final class CommandHttpStatus {

    private CommandHttpStatus() {
    }

    /**
     * {@code 202 Accepted} quando o comando é novo: foi aceito, mas o Worker
     * ainda vai processá-lo. {@code 200 OK} quando é um reenvio: a resposta
     * traz o comando original.
     *
     * @param created {@code true} se esta requisição criou o comando
     * @return o status a responder
     */
    public static HttpStatus of(boolean created) {
        if (created) {
            return HttpStatus.ACCEPTED;
        }

        return HttpStatus.OK;
    }
}
