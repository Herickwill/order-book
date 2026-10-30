package com.meli.orderbook.api.common;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageTest {

    @Test
    void should_have_next_page_when_lookahead_row_is_present() {
        var rowsWithLookahead = List.of(1, 2, 3);

        var page = Page.fromLookahead(rowsWithLookahead, 0, 2);

        assertThat(page.items()).containsExactly(1, 2);
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    void should_not_have_next_page_when_rows_fit_in_the_page() {
        var page = Page.fromLookahead(List.of(1, 2), 0, 2);

        assertThat(page.items()).containsExactly(1, 2);
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    void should_return_empty_page_when_there_are_no_rows() {
        var page = Page.<Integer>fromLookahead(List.of(), 3, 20);

        assertThat(page.items()).isEmpty();
        assertThat(page.hasNext()).isFalse();
        assertThat(page.page()).isEqualTo(3);
        assertThat(page.size()).isEqualTo(20);
    }

    @Test
    void should_ask_for_one_row_more_than_the_page_size() {
        assertThat(Page.limitWithLookahead(20)).isEqualTo(21);
    }

    @Test
    void should_compute_offset_without_int_overflow() {
        assertThat(Page.offsetOf(2, 20)).isEqualTo(40L);
        assertThat(Page.offsetOf(Integer.MAX_VALUE, 100)).isEqualTo(214_748_364_700L);
    }

    @Test
    void should_expose_items_as_an_unmodifiable_list() {
        var page = Page.fromLookahead(List.of(1, 2, 3), 0, 2);

        assertThatThrownBy(() -> page.items().add(4)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void should_keep_pagination_when_items_are_mapped() {
        var page = Page.fromLookahead(List.of(1, 2, 3), 4, 2);

        var mapped = page.map(number -> "n" + number);

        assertThat(mapped.items()).containsExactly("n1", "n2");
        assertThat(mapped.page()).isEqualTo(4);
        assertThat(mapped.size()).isEqualTo(2);
        assertThat(mapped.hasNext()).isTrue();
    }
}
