package io.github.phunguy65.ttbs.backend.shared.infrastructure.web.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PageRequest(
        @Min(0) int page, @Min(1) @Max(100) int size) {

    public PageRequest() {
        this(0, 20);
    }
}
