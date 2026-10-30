-- Saldo por usuario e ativo. Escrita apenas pelo Worker.
-- A carteira nasce no primeiro DEPOSIT; nao ha cadastro previo.
CREATE TABLE wallets (
    user_id   VARCHAR(100)  NOT NULL,
    asset     VARCHAR(3)    NOT NULL,
    available DECIMAL(17,2) NOT NULL DEFAULT 0,
    reserved  DECIMAL(17,2) NOT NULL DEFAULT 0,

    CONSTRAINT pk_wallets
        PRIMARY KEY (user_id, asset),
    CONSTRAINT ck_wallets_asset
        CHECK (asset IN ('BRL', 'VIB')),
    CONSTRAINT ck_wallets_available
        CHECK (available >= 0),
    CONSTRAINT ck_wallets_reserved
        CHECK (reserved >= 0),
    -- Vibranium e negociado em unidades inteiras.
    CONSTRAINT ck_wallets_vib_integral
        CHECK (asset <> 'VIB'
            OR (available = trunc(available) AND reserved = trunc(reserved)))
);
