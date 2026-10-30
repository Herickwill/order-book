-- ============================================================================
-- APAGA TODAS AS TABELAS DO ORDER BOOK, INCLUSIVE OS DADOS.
-- Uso exclusivo em ambiente local de desenvolvimento.
--
-- Fica em db/dev/ de proposito: `db/*.sql` nao inclui subpastas, entao este
-- script nunca roda junto com a criacao do schema por engano.
--
-- Uso, na raiz do projeto com o docker compose no ar, apagando e recriando
-- numa unica transacao. Se qualquer passo falhar, nada muda no banco:
--
--   cat db/dev/reset-schema.sql db/0*.sql | docker compose exec -T postgres \
--     psql -U order_book -d order_book -v ON_ERROR_STOP=1 --single-transaction
-- ============================================================================

-- Em ordem inversa de dependencia (trades tem FK para orders).
DROP TABLE IF EXISTS trades;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS wallets;
DROP TABLE IF EXISTS commands;
