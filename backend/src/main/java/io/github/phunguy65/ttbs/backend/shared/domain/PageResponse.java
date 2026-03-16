package io.github.phunguy65.ttbs.backend.shared.domain;

import java.util.List;

/**
 * A page of results with total count.
 *
 * <p>Pure Java domain abstraction for offset-based Page pagination. Consistent with other shared
 * domain types ({@link Result}, {@link Money}, {@link UserId}) – zero Spring/JPA dependencies.
 *
 * <p>The factory method {@code of(...)} is used by infrastructure adapters to build a
 * {@code PageResponse} from a Spring Data {@code Page<Entity>} after entity-to-domain mapping.
 * Infrastructure code is responsible for the mapping; domain code only consumes this record.
 *
 * @param <T> the domain type for each item in the page
 */
public record PageResponse<T>(
        List<T> content, int page, int size, boolean hasNext, boolean hasPrevious, long total) {

    /**
     * Compact canonical constructor – defensively copies {@code content} to make the record
     * immutable.
     */
    public PageResponse {
        content = List.copyOf(content);
    }

    /**
     * Factory for infrastructure adapters.
     *
     * <p>Infrastructure code should:
     * <ol>
     *   <li>Map domain objects from entity objects.
     *   <li>Call this factory with the mapped list, page metadata, the {@code hasNext} flag
     *       and {@code total} derived from Spring Data's {@code Page}.
     * </ol>
     *
     * @param content     already-mapped domain items
     * @param page        0-indexed page number
     * @param size        requested page size
     * @param hasNext     {@code true} if more pages exist (from {@code Page.hasNext()})
     * @param total       total number of items across all pages (from {@code Page.getTotalElements()})
     */
    public static <T> PageResponse<T> of(
            List<T> content, int page, int size, boolean hasNext, long total) {
        boolean hasPrevious = page > 0;
        return new PageResponse<>(content, page, size, hasNext, hasPrevious, total);
    }

    /** Convenience factory for an empty result set on page 0. */
    public static <T> PageResponse<T> empty(int size) {
        return new PageResponse<>(List.of(), 0, size, false, false, 0L);
    }
}
