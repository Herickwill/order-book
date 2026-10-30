package com.meli.orderbook.worker.persistence;

import com.meli.orderbook.worker.processing.Command;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Lê os comandos pendentes de {@code commands} (o inbox), em ordem de id.
 *
 * <p>Busca por {@code status = 'RECEIVED'}, e não por {@code id > último}: um
 * insert da API que pegou um id menor e fez commit depois aparece no lote
 * seguinte e nunca é pulado. O filtro é o mesmo do índice parcial
 * {@code ix_commands_pending}, então a leitura usa o índice.
 */
@Repository
public class CommandReader {

    private static final String READ_PENDING = """
            SELECT id, user_id, type, order_id, side, order_type,
                   price, quantity, asset, amount, created_at
            FROM commands
            WHERE status = 'RECEIVED'
            ORDER BY id
            LIMIT :limit
            """;

    private static final CommandRowMapper ROW_MAPPER = new CommandRowMapper();

    private final JdbcClient jdbcClient;

    public CommandReader(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /**
     * Os próximos comandos a processar.
     *
     * @param limit o tamanho máximo do lote
     * @return até {@code limit} comandos, do menor id para o maior; vazio se a fila está vazia
     */
    public List<Command> readPending(int limit) {
        return jdbcClient.sql(READ_PENDING)
                .param("limit", limit)
                .query(ROW_MAPPER)
                .list();
    }
}
