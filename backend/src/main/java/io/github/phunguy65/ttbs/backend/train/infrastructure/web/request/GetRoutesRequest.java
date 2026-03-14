package io.github.phunguy65.ttbs.backend.train.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.train.application.query.GetRoutesQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record GetRoutesRequest(
        @Min(0) int page, @Min(1) @Max(100) int size) {

    public GetRoutesRequest() {
        this(0, 20);
    }

    public GetRoutesQuery toQuery() {
        return new GetRoutesQuery(page, size);
    }
}
