# Acompanha as atualizacoes estaveis da serie PostgreSQL 18.
FROM postgres:18

ENV POSTGRES_USER=order_book \
    POSTGRES_DB=order_book

EXPOSE 5432

HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=5 \
    CMD pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB" || exit 1
