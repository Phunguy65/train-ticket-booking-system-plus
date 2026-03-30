package io.github.phunguy65.ttbs.backend.train.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.request.PagedRequest;
import io.github.phunguy65.ttbs.backend.train.application.query.GetRouteTemplatesQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record GetRouteTemplatesRequest(
        @Min(0) int page, @Min(1) @Max(100) int size) implements PagedRequest {

    public GetRouteTemplatesRequest() {
        this(0, 20);
    }

    public GetRouteTemplatesQuery toQuery() {
        return new GetRouteTemplatesQuery(page, size);
    }
}
