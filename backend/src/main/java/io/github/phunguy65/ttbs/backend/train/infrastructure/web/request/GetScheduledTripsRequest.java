package io.github.phunguy65.ttbs.backend.train.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.request.PagedRequest;
import io.github.phunguy65.ttbs.backend.train.application.query.GetScheduledTripsQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "Offset-based pagination query for scheduled trips.")
public record GetScheduledTripsRequest(
        @Schema(description = "Zero-based page index.", minimum = "0", example = "0") @Min(0) Integer page,

        @Schema(
                description = "Number of scheduled trips per page.",
                minimum = "1",
                maximum = "100",
                example = "20")
        @Min(1) @Max(100) Integer size)
        implements PagedRequest {

    public GetScheduledTripsRequest {
        if (page == null) {
            page = 0;
        }
        if (size == null) {
            size = 20;
        }
    }

    public GetScheduledTripsRequest() {
        this(0, 20);
    }

    public GetScheduledTripsQuery toQuery() {
        return new GetScheduledTripsQuery(page, size);
    }
}
