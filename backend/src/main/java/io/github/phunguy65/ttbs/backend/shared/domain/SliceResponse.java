package io.github.phunguy65.ttbs.backend.shared.domain;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Cursor-based pagination wrapper.")
public record SliceResponse<T>(
        @ArraySchema(schema = @Schema(description = "Items in the current slice."))
        List<T> content,

        @Schema(description = "Requested slice size.", minimum = "1", example = "20")
        int size,

        @Schema(description = "Whether another slice exists after this one.")
        boolean hasNext,

        @Schema(
                description = "Opaque cursor for the next slice. Null when no more results exist.",
                example = "opaque-cursor-token",
                nullable = true,
                types = {"string", "null"})
        String nextCursor) {

    public SliceResponse {
        content = List.copyOf(content);
    }

    public static <T> SliceResponse<T> of(
            List<T> content, int size, boolean hasNext, String nextCursor) {
        return new SliceResponse<>(content, size, hasNext, nextCursor);
    }

    public static <T> SliceResponse<T> empty(int size) {
        return new SliceResponse<>(List.of(), size, false, null);
    }
}
