package io.github.phunguy65.ttbs.backend.train.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.request.PagedRequest;
import io.github.phunguy65.ttbs.backend.train.application.query.GetRouteTemplatesQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "Offset-based pagination query for route templates.")
public record GetRouteTemplatesRequest(
        @Schema(description = "Zero-based page index.", minimum = "0", example = "0") @Min(0) Integer page,

        @Schema(
                description = "Number of route templates per page.",
                minimum = "1",
                maximum = "100",
                example = "20")
        @Min(1) @Max(100) Integer size)
        implements PagedRequest {

    public GetRouteTemplatesRequest {
        if (page == null) {
            page = 0;
        }
        if (size == null) {
            size = 20;
        }
    }

    public GetRouteTemplatesRequest() {
        this(0, 20);
    }

    public GetRouteTemplatesQuery toQuery() {
        return new GetRouteTemplatesQuery(page, size);
    }
}
