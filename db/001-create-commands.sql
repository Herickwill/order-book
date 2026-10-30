-- Inbox de comandos. Unica tabela escrita pela API.
-- Guarda NEW_ORDER, CANCEL_ORDER e DEPOSIT.
CREATE TABLE commands (
    id              BIGINT GENERATED ALWAYS AS IDENTITY,
    user_id         VARCHAR(100)  NOT NULL,
    -- Prefixada pelo tipo de comando (order:, deposit:, cancel:) para que chaves
    -- de tipos diferentes nunca colidam. 150 do cliente + o maior prefixo.
    idempotency_key VARCHAR(160)  NOT NULL,
    type            VARCHAR(20)   NOT NULL,
    status          VARCHAR(20)   NOT NULL,
    -- NEW_ORDER / CANCEL_ORDER
    order_id        UUID,
    side            VARCHAR(4),
    order_type      VARCHAR(6),
    price           DECIMAL(17,2),
    quantity        BIGINT,
    -- DEPOSIT
    asset           VARCHAR(3),
    amount          DECIMAL(17,2),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_commands
        PRIMARY KEY (id),
    CONSTRAINT uq_commands_idempotency
        UNIQUE (user_id, idempotency_key),
    CONSTRAINT ck_commands_type
        CHECK (type IN ('NEW_ORDER', 'CANCEL_ORDER', 'DEPOSIT')),
    CONSTRAINT ck_commands_status
        CHECK (status IN ('RECEIVED', 'PROCESSED')),

    CONSTRAINT ck_commands_new_order
        CHECK (type <> 'NEW_ORDER' OR (
            order_id   IS NOT NULL
        AND side       IN ('BUY', 'SELL')
        AND order_type IN ('LIMIT', 'MARKET')
        AND quantity   IS NOT NULL AND quantity > 0
        AND ((order_type = 'LIMIT'  AND price IS NOT NULL AND price > 0)
          OR (order_type = 'MARKET' AND price IS NULL))
        )),
    CONSTRAINT ck_commands_cancel_order
        CHECK (type <> 'CANCEL_ORDER' OR order_id IS NOT NULL),
    CONSTRAINT ck_commands_deposit
        CHECK (type <> 'DEPOSIT' OR (
            asset  IN ('BRL', 'VIB')
        AND amount IS NOT NULL AND amount > 0
        AND (asset <> 'VIB' OR amount = trunc(amount))
        ))
);

-- O Worker busca exatamente este predicado a cada ciclo. A tabela cresce para
-- sempre (e o registro historico dos depositos), entao o indice e parcial:
-- so os pendentes sao indexados, e o tamanho do indice acompanha a fila, nao a
-- tabela.
CREATE INDEX ix_commands_pending
    ON commands (id)
    WHERE status = 'RECEIVED';

-- GET /orders/{orderId} cai aqui quando a ordem ainda nao foi processada pelo
-- Worker. Parcial: so as linhas NEW_ORDER tem order_id que interessa a busca.
CREATE INDEX ix_commands_order_id
    ON commands (order_id)
    WHERE type = 'NEW_ORDER';
