package com.meli.orderbook.api.order.repository;

import com.meli.orderbook.shared.order.OrderSide;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * {@link OrderRepository} sobre o Postgres, com {@code JdbcClient}.
 *
 * <p>As ordens abertas são lidas pelo índice parcial {@code ix_orders_open_book}
 * ({@code side, price, seq}), que já entrega a prioridade de execução. O filtro
 * por usuário não tem índice próprio: o Postgres percorre o lado do livro na
 * ordem do índice e descarta as ordens de outros usuários. Com um livro muito
 * grande, a evolução é um índice parcial começando por {@code user_id}.
 */
@Repository
public class PostgresOrderRepository implements OrderRepository {

    private static final String FIND_BY_ID = """
            SELECT id, user_id, seq, side, type, price, quantity,
                   filled_quantity, status, reject_reason, created_at
            FROM orders
            WHERE id = :orderId
            """;

    /** Vendas: menor preço primeiro; no mesmo preço, quem chegou antes (seq). */
    private static final String FIND_OPEN_ASKS = """
            SELECT id, user_id, seq, side, type, price, quantity,
                   filled_quantity, status, reject_reason, created_at
            FROM orders
            WHERE side = 'SELL'
              AND status IN ('NEW', 'PARTIALLY_FILLED')
              AND (:userId IS NULL OR user_id = :userId)
            ORDER BY price ASC, seq ASC
            LIMIT :limit OFFSET :offset
            """;

    /** Compras: maior preço primeiro; no mesmo preço, quem chegou antes (seq). */
    private static final String FIND_OPEN_BIDS = """
            SELECT id, user_id, seq, side, type, price, quantity,
                   filled_quantity, status, reject_reason, created_at
            FROM orders
            WHERE side = 'BUY'
              AND status IN ('NEW', 'PARTIALLY_FILLED')
              AND (:userId IS NULL OR user_id = :userId)
            ORDER BY price DESC, seq ASC
            LIMIT :limit OFFSET :offset
            """;

    private static final OrderRowMapper ROW_MAPPER = new OrderRowMapper();

    private final JdbcClient jdbcClient;

    public PostgresOrderRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<OrderRow> findById(UUID orderId) {
        return jdbcClient.sql(FIND_BY_ID)
                .param("orderId", orderId, Types.OTHER)
                .query(ROW_MAPPER)
                .optional();
    }

    @Override
    public List<OrderRow> findOpen(OrderSide side, String userId, int limit, long offset) {
        var query = switch (side) {
            case SELL -> FIND_OPEN_ASKS;
            case BUY -> FIND_OPEN_BIDS;
        };

        return jdbcClient.sql(query)
                .param("userId", userId, Types.VARCHAR)
                .param("limit", limit)
                .param("offset", offset)
                .query(ROW_MAPPER)
                .list();
    }
}
