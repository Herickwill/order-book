package com.meli.orderbook.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Ponto de entrada do Worker, o único processo que altera ordens, trades e
 * saldos.
 *
 * <p>Os beans do Spring ficam aqui e em {@code persistence}. O núcleo
 * ({@code matching}, {@code settlement} e {@code processing}) é Java puro, sem
 * anotações: o {@link WorkerLoop} monta o {@code CommandProcessor} depois de
 * reconstruir o estado do banco.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class OrderBookWorkerApplication {

    /**
     * Sobe o Worker.
     *
     * @param args argumentos de linha de comando repassados ao Spring
     */
    public static void main(String[] args) {
        SpringApplication.run(OrderBookWorkerApplication.class, args);
    }
}
