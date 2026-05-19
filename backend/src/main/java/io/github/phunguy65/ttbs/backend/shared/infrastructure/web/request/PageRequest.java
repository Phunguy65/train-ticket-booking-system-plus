package io.github.phunguy65.ttbs.backend.shared.infrastructure.web.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PageRequest(
        @Min(0) Integer page, @Min(1) @Max(100) Integer size) implements PagedRequest {

    public PageRequest {
        if (page == null) {
            page = 0;
        }
        if (size == null) {
            size = 20;
        }
    }

    public PageRequest() {
        this(0, 20);
    }
}
