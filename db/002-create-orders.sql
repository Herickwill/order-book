-- Estado das ordens. Escrita apenas pelo Worker.
CREATE TABLE orders (
    id              UUID          NOT NULL,
    user_id         VARCHAR(100)  NOT NULL,
    -- Contador de chegada atribuido pelo Worker. Desempata o FIFO dentro de um
    -- nivel de preco e reconstroi a fila exata apos restart.
    -- NAO e chave estrangeira para commands.
    seq             BIGINT        NOT NULL,
    side            VARCHAR(4)    NOT NULL,
    type            VARCHAR(6)    NOT NULL,
    price           DECIMAL(17,2),
    quantity        BIGINT        NOT NULL,
    filled_quantity BIGINT        NOT NULL DEFAULT 0,
    status          VARCHAR(20)   NOT NULL,
    reject_reason   VARCHAR(60),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_orders
        PRIMARY KEY (id),
    CONSTRAINT uq_orders_seq
        UNIQUE (seq),
    CONSTRAINT ck_orders_side
        CHECK (side IN ('BUY', 'SELL')),
    CONSTRAINT ck_orders_type
        CHECK (type IN ('LIMIT', 'MARKET')),
    CONSTRAINT ck_orders_status
        CHECK (status IN ('NEW', 'PARTIALLY_FILLED', 'FILLED', 'CANCELLED', 'REJECTED')),
    CONSTRAINT ck_orders_quantity
        CHECK (quantity > 0),
    CONSTRAINT ck_orders_filled_quantity
        CHECK (filled_quantity >= 0 AND filled_quantity <= quantity),
    CONSTRAINT ck_orders_price
        CHECK ((type = 'LIMIT'  AND price IS NOT NULL AND price > 0)
            OR (type = 'MARKET' AND price IS NULL)),
    -- reject_reason existe se e somente se a ordem foi rejeitada.
    CONSTRAINT ck_orders_reject_reason
        CHECK ((status = 'REJECTED') = (reject_reason IS NOT NULL)),
    CONSTRAINT ck_orders_reject_reason_value
        CHECK (reject_reason IS NULL
            OR reject_reason IN ('INSUFFICIENT_FUNDS', 'NO_LIQUIDITY'))
);

-- Reconstrucao do livro ao subir e GET /orders?side=: so as ordens abertas
-- interessam, ja na ordem de prioridade (preco, depois seq).
CREATE INDEX ix_orders_open_book
    ON orders (side, price, seq)
    WHERE status IN ('NEW', 'PARTIALLY_FILLED');
