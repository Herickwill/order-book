package com.meli.orderbook.api.common;

import java.util.List;
import java.util.function.Function;

/**
 * Uma página de resultados.
 *
 * <p>Para saber se existe próxima página sem fazer um {@code COUNT(*)}, que
 * fica caro numa tabela que só cresce, a busca pede uma linha a mais do que o
 * tamanho da página. Se essa linha vier, existe próxima página, e ela é
 * descartada da resposta.
 *
 * @param items   os itens da página, no máximo {@code size}
 * @param page    o número da página, começando em zero
 * @param size    o tamanho de página pedido
 * @param hasNext {@code true} se existe ao menos mais um item depois desta página
 * @param <T>     o tipo de cada item
 */
public record Page<T>(List<T> items, int page, int size, boolean hasNext) {

    /** Página devolvida quando o cliente não informa {@code page}. */
    public static final int FIRST_PAGE = 0;

    /** Tamanho usado quando o cliente não informa {@code size}. */
    public static final int DEFAULT_SIZE = 20;

    /** Maior tamanho de página aceito. */
    public static final int MAX_SIZE = 100;

    /**
     * Quantas linhas pular para chegar à página pedida. Calculado em
     * {@code long} para não estourar com páginas altas.
     *
     * @param page o número da página, começando em zero
     * @param size o tamanho da página
     * @return o deslocamento da busca
     */
    public static long offsetOf(int page, int size) {
        return (long) page * size;
    }

    /**
     * O limite da busca: o tamanho da página mais a linha que revela se há
     * próxima página.
     *
     * @param size o tamanho da página
     * @return {@code size + 1}
     */
    public static int limitWithLookahead(int size) {
        return size + 1;
    }

    /**
     * Monta a página a partir de uma busca feita com {@link #limitWithLookahead(int)}.
     *
     * @param rowsWithLookahead as linhas devolvidas pela busca, até {@code size + 1}
     * @param page              o número da página pedida
     * @param size              o tamanho da página pedida
     * @param <T>               o tipo de cada linha
     * @return a página, sem a linha extra
     */
    public static <T> Page<T> fromLookahead(List<T> rowsWithLookahead, int page, int size) {
        var hasNext = rowsWithLookahead.size() > size;
        var items = hasNext ? rowsWithLookahead.subList(0, size) : rowsWithLookahead;

        return new Page<>(List.copyOf(items), page, size, hasNext);
    }

    /**
     * Converte cada item, mantendo a paginação.
     *
     * @param mapper a conversão de um item
     * @param <R>    o tipo convertido
     * @return uma nova página com os itens convertidos
     */
    public <R> Page<R> map(Function<? super T, ? extends R> mapper) {
        List<R> mappedItems = items.stream()
                .<R>map(mapper)
                .toList();

        return new Page<>(mappedItems, page, size, hasNext);
    }
}
