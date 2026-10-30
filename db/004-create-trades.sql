-- Historico de execucoes. Escrita apenas pelo Worker.
CREATE TABLE trades (
    id            BIGINT        GENERATED ALWAYS AS IDENTITY,
    buy_order_id  UUID          NOT NULL,
    sell_order_id UUID          NOT NULL,
    -- Desnormalizados de proposito: GET /trades?userId= e o extrato da
    -- carteira varrem por usuario, e sem estas colunas cada consulta exigiria dois
    -- joins com orders.
    buy_user_id   VARCHAR(100)  NOT NULL,
    sell_user_id  VARCHAR(100)  NOT NULL,
    price         DECIMAL(17,2) NOT NULL,
    quantity      BIGINT        NOT NULL,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_trades
        PRIMARY KEY (id),
    CONSTRAINT fk_trades_buy_order
        FOREIGN KEY (buy_order_id)  REFERENCES orders (id),
    CONSTRAINT fk_trades_sell_order
        FOREIGN KEY (sell_order_id) REFERENCES orders (id),
    CONSTRAINT ck_trades_price
        CHECK (price > 0),
    CONSTRAINT ck_trades_quantity
        CHECK (quantity > 0),
    CONSTRAINT ck_trades_distinct_orders
        CHECK (buy_order_id <> sell_order_id)
);

CREATE INDEX ix_trades_buy_user  ON trades (buy_user_id, id);
CREATE INDEX ix_trades_sell_user ON trades (sell_user_id, id);
