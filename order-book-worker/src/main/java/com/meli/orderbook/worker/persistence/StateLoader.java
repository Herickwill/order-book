package com.meli.orderbook.worker.persistence;

import com.meli.orderbook.worker.matching.OrderBook;
import com.meli.orderbook.worker.settlement.Wallets;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Reconstrói o livro e as carteiras a partir do banco.
 *
 * <p>É também a recuperação depois de qualquer falha: a memória é uma cópia do
 * banco, e o banco é a verdade. Só pode rodar <b>depois</b> do advisory lock;
 * antes dele, outro Worker ainda poderia estar escrevendo.
 *
 * <p>As ordens abertas voltam ao livro em ordem de {@code seq}, cada uma com o
 * seu {@code quantity - filled_quantity}. Como a fila de cada preço segue a
 * ordem de inserção, isso reproduz a fila exata de antes da queda: um maker
 * parcial volta para a cabeça, e quem entrou depois volta para trás dele. Não
 * há replay: os comandos ainda {@code RECEIVED} são processados normalmente
 * pelo loop.
 */
@Component
public class StateLoader {

    private static final String LOAD_WALLETS = """
            SELECT user_id, asset, available, reserved
            FROM wallets
            """;

    private static final String LOAD_OPEN_ORDERS = """
            SELECT id, user_id, seq, side, type, price, quantity,
                   filled_quantity, status, created_at
            FROM orders
            WHERE status IN ('NEW', 'PARTIALLY_FILLED')
            ORDER BY seq
            """;

    private static final String LOAD_LAST_SEQ = """
            SELECT COALESCE(MAX(seq), 0)
            FROM orders
            """;

    private static final WalletRowMapper WALLET_ROW_MAPPER = new WalletRowMapper();
    private static final OpenOrderRowMapper OPEN_ORDER_ROW_MAPPER = new OpenOrderRowMapper();

    private final JdbcClient jdbcClient;

    public StateLoader(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /**
     * Lê o estado gravado e monta as estruturas em memória.
     *
     * @return o livro, as carteiras e o último {@code seq}
     * @throws IllegalArgumentException se uma ordem gravada não pode voltar ao
     *                                  livro (estado incoerente com a quantidade executada)
     */
    public RestoredState load() {
        var wallets = new Wallets();
        var storedWallets = jdbcClient.sql(LOAD_WALLETS).query(WALLET_ROW_MAPPER).list();
        for (var wallet : storedWallets) {
            wallets.restore(wallet);
        }

        var book = new OrderBook();
        var openOrders = jdbcClient.sql(LOAD_OPEN_ORDERS).query(OPEN_ORDER_ROW_MAPPER).list();
        for (var order : openOrders) {
            book.add(order);
        }

        var lastSeq = jdbcClient.sql(LOAD_LAST_SEQ).query(Long.class).single();
        return new RestoredState(book, wallets, lastSeq, openOrders.size());
    }
}
