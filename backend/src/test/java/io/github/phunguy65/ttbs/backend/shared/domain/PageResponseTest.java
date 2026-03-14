package io.github.phunguy65.ttbs.backend.shared.domain;

import static org.assertj.core.api.Assertions.*;

import io.github.phunguy65.ttbs.backend.shared.application.response.PageResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class PageResponseTest {

    // ── PageResponse.of() ──────────────────────────────────────────────────────

    @Test
    void of_firstPageWithMoreData_hasNextTrueHasPreviousFalse() {
        List<String> items = List.of("a", "b", "c");

        PageResponse<String> result = PageResponse.of(items, 0, 20, true);

        assertThat(result.hasNext()).isTrue();
        assertThat(result.hasPrevious()).isFalse();
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.content()).containsExactly("a", "b", "c");
    }

    @Test
    void of_firstPageNoMoreData_hasNextFalseHasPreviousFalse() {
        List<String> items = List.of("only");

        PageResponse<String> result = PageResponse.of(items, 0, 20, false);

        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isFalse();
    }

    @Test
    void of_middlePage_hasPreviousTrueHasNextTrue() {
        List<String> items = List.of("x", "y");

        PageResponse<String> result = PageResponse.of(items, 2, 10, true);

        assertThat(result.hasPrevious()).isTrue();
        assertThat(result.hasNext()).isTrue();
        assertThat(result.page()).isEqualTo(2);
    }

    @Test
    void of_lastPage_hasPreviousTrueHasNextFalse() {
        List<String> items = List.of("last");

        PageResponse<String> result = PageResponse.of(items, 3, 10, false);

        assertThat(result.hasPrevious()).isTrue();
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void of_emptyItems_bothFlagsAreFalseOnFirstPage() {
        PageResponse<String> result = PageResponse.of(List.of(), 0, 20, false);

        assertThat(result.content()).isEmpty();
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isFalse();
    }

    // ── PageResponse.empty() ───────────────────────────────────────────────────

    @Test
    void empty_returnsZeroPageWithNoFlags() {
        PageResponse<Integer> result = PageResponse.empty(50);

        assertThat(result.content()).isEmpty();
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(50);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isFalse();
    }

    // ── Immutability ─────────────────────────────────────────────────────────

    @Test
    void items_listIsImmutable() {
        List<String> mutable = new java.util.ArrayList<>(List.of("a"));
        PageResponse<String> result = PageResponse.of(mutable, 0, 10, false);

        // mutation of original list does not affect PageResponse
        mutable.add("b");

        assertThat(result.content()).hasSize(1).containsExactly("a");
    }
}
