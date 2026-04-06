package io.github.phunguy65.ttbs.backend.shared.domain;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Offset-based pagination wrapper.")
public record PageResponse<T>(
        @ArraySchema(schema = @Schema(description = "Items in the current page."))
        List<T> content,

        @Schema(description = "Zero-based page index.", minimum = "0", example = "0")
        int page,

        @Schema(description = "Requested page size.", minimum = "1", example = "20")
        int size,

        @Schema(description = "Whether a subsequent page exists.")
        boolean hasNext,

        @Schema(description = "Whether a previous page exists.")
        boolean hasPrevious,

        @Schema(
                description = "Total number of matching items across all pages.",
                minimum = "0",
                example = "125")
        long total) {

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
