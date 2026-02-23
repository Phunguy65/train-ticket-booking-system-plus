package io.github.phunguy65.ttbs.backend.shared.domain;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class PageResultTest {

    // ── PageResult.of() ──────────────────────────────────────────────────────

    @Test
    void of_firstPageWithMoreData_hasNextTrueHasPreviousFalse() {
        List<String> items = List.of("a", "b", "c");

        PageResult<String> result = PageResult.of(items, 0, 20, true);

        assertThat(result.hasNext()).isTrue();
        assertThat(result.hasPrevious()).isFalse();
        assertThat(result.pageNumber()).isEqualTo(0);
        assertThat(result.pageSize()).isEqualTo(20);
        assertThat(result.items()).containsExactly("a", "b", "c");
    }

    @Test
    void of_firstPageNoMoreData_hasNextFalseHasPreviousFalse() {
        List<String> items = List.of("only");

        PageResult<String> result = PageResult.of(items, 0, 20, false);

        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isFalse();
    }

    @Test
    void of_middlePage_hasPreviousTrueHasNextTrue() {
        List<String> items = List.of("x", "y");

        PageResult<String> result = PageResult.of(items, 2, 10, true);

        assertThat(result.hasPrevious()).isTrue();
        assertThat(result.hasNext()).isTrue();
        assertThat(result.pageNumber()).isEqualTo(2);
    }

    @Test
    void of_lastPage_hasPreviousTrueHasNextFalse() {
        List<String> items = List.of("last");

        PageResult<String> result = PageResult.of(items, 3, 10, false);

        assertThat(result.hasPrevious()).isTrue();
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void of_emptyItems_bothFlagsAreFalseOnFirstPage() {
        PageResult<String> result = PageResult.of(List.of(), 0, 20, false);

        assertThat(result.items()).isEmpty();
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isFalse();
    }

    // ── PageResult.empty() ───────────────────────────────────────────────────

    @Test
    void empty_returnsZeroPageWithNoFlags() {
        PageResult<Integer> result = PageResult.empty(50);

        assertThat(result.items()).isEmpty();
        assertThat(result.pageNumber()).isEqualTo(0);
        assertThat(result.pageSize()).isEqualTo(50);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isFalse();
    }

    // ── Immutability ─────────────────────────────────────────────────────────

    @Test
    void items_listIsImmutable() {
        List<String> mutable = new java.util.ArrayList<>(List.of("a"));
        PageResult<String> result = PageResult.of(mutable, 0, 10, false);

        // mutation of original list does not affect PageResult
        mutable.add("b");

        assertThat(result.items()).hasSize(1).containsExactly("a");
    }
}
