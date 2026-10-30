package com.meli.orderbook.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada da API do livro de ofertas.
 *
 * <p>A API só grava comandos em {@code commands} e lê as demais
 * tabelas. Quem altera saldo, ordem e trade é o Worker.
 */
@SpringBootApplication
public class OrderBookApiApplication {

    /**
     * Sobe a aplicação Spring Boot.
     *
     * @param args argumentos de linha de comando repassados ao Spring
     */
    public static void main(String[] args) {
        SpringApplication.run(OrderBookApiApplication.class, args);
    }
}
