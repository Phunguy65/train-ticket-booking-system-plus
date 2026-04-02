package io.github.phunguy65.ttbs.backend.shared.domain;

import java.util.List;

public record SliceResponse<T>(List<T> content, int size, boolean hasNext, String nextCursor) {

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
