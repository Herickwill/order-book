package com.meli.orderbook.api.command.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.Optional;
import java.util.UUID;

/** {@link CommandRepository} sobre o Postgres, com {@code JdbcClient}. */
@Repository
public class PostgresCommandRepository implements CommandRepository {

    private static final String INSERT_IF_ABSENT = """
            INSERT INTO commands
                (user_id, idempotency_key, type, status, order_id,
                 side, order_type, price, quantity, asset, amount)
            VALUES
                (:userId, :idempotencyKey, :type, :status, :orderId,
                 :side, :orderType, :price, :quantity, :asset, :amount)
            ON CONFLICT (user_id, idempotency_key) DO NOTHING
            """;

    private static final String FIND_BY_IDEMPOTENCY = """
            SELECT id, user_id, idempotency_key, type, status, order_id,
                   side, order_type, price, quantity, asset, amount, created_at
            FROM commands
            WHERE user_id = :userId
              AND idempotency_key = :idempotencyKey
            """;

    /** Usa o índice parcial {@code ix_commands_order_id}, que só cobre NEW_ORDER. */
    private static final String FIND_NEW_ORDER_BY_ORDER_ID = """
            SELECT id, user_id, idempotency_key, type, status, order_id,
                   side, order_type, price, quantity, asset, amount, created_at
            FROM commands
            WHERE order_id = :orderId
              AND type = 'NEW_ORDER'
            """;

    private static final CommandRowMapper ROW_MAPPER = new CommandRowMapper();

    private final JdbcClient jdbcClient;

    public PostgresCommandRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public int insertIfAbsent(CommandRow command) {
        return jdbcClient.sql(INSERT_IF_ABSENT)
                .param("userId", command.userId())
                .param("idempotencyKey", command.idempotencyKey())
                .param("type", nameOf(command.type()), Types.VARCHAR)
                .param("status", nameOf(command.status()), Types.VARCHAR)
                .param("orderId", command.orderId(), Types.OTHER)
                .param("side", nameOf(command.side()), Types.VARCHAR)
                .param("orderType", nameOf(command.orderType()), Types.VARCHAR)
                .param("price", command.price(), Types.NUMERIC)
                .param("quantity", command.quantity(), Types.BIGINT)
                .param("asset", nameOf(command.asset()), Types.VARCHAR)
                .param("amount", command.amount(), Types.NUMERIC)
                .update();
    }

    @Override
    public Optional<CommandRow> findByIdempotency(String userId, String idempotencyKey) {
        return jdbcClient.sql(FIND_BY_IDEMPOTENCY)
                .param("userId", userId)
                .param("idempotencyKey", idempotencyKey)
                .query(ROW_MAPPER)
                .optional();
    }

    @Override
    public Optional<CommandRow> findNewOrderByOrderId(UUID orderId) {
        return jdbcClient.sql(FIND_NEW_ORDER_BY_ORDER_ID)
                .param("orderId", orderId, Types.OTHER)
                .query(ROW_MAPPER)
                .optional();
    }

    /** Enums são gravados pelo nome; colunas de outro tipo de comando ficam nulas. */
    private static String nameOf(Enum<?> value) {
        if (value == null) {
            return null;
        }

        return value.name();
    }
}
