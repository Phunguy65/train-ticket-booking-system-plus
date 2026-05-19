package io.github.phunguy65.ttbs.backend.train.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.request.PagedRequest;
import io.github.phunguy65.ttbs.backend.train.application.query.GetTrainsQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "Offset-based pagination query for trains.")
public record GetTrainsRequest(
        @Schema(description = "Zero-based page index.", minimum = "0", example = "0") @Min(0) Integer page,

        @Schema(
                description = "Number of trains per page.",
                minimum = "1",
                maximum = "100",
                example = "20")
        @Min(1) @Max(100) Integer size)
        implements PagedRequest {

    public GetTrainsRequest {
        if (page == null) {
            page = 0;
        }
        if (size == null) {
            size = 20;
        }
    }

    public GetTrainsRequest() {
        this(0, 20);
    }

    public GetTrainsQuery toQuery() {
        return new GetTrainsQuery(page, size);
    }
}
