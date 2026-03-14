package io.github.phunguy65.ttbs.backend.shared.domain;

/**
 * Domain-owned sort specification — zero Spring/JPA dependencies.
 *
 * <p>Use {@link #asc(String)} / {@link #desc(String)} factory methods to build sort orders.
 * Infrastructure adapters are responsible for converting {@code SortOrder} to framework-specific
 * types (e.g., {@code org.springframework.data.domain.Sort}).
 */
public record SortOrder(String field, Direction direction) {

    public enum Direction {
        ASC,
        DESC
    }

    public static SortOrder asc(String field) {
        return new SortOrder(field, Direction.ASC);
    }

    public static SortOrder desc(String field) {
        return new SortOrder(field, Direction.DESC);
    }
}
