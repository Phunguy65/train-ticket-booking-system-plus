package io.github.phunguy65.ttbs.backend.station.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.request.PagedRequest;
import io.github.phunguy65.ttbs.backend.station.application.query.GetStationsQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "Offset-based pagination query for stations.")
public record GetStationsRequest(
        @Schema(description = "Zero-based page index.", minimum = "0", example = "0") @Min(0) Integer page,

        @Schema(
                description = "Number of stations per page.",
                minimum = "1",
                maximum = "100",
                example = "20")
        @Min(1) @Max(100) Integer size)
        implements PagedRequest {

    public GetStationsRequest {
        if (page == null) {
            page = 0;
        }
        if (size == null) {
            size = 20;
        }
    }

    public GetStationsRequest() {
        this(0, 20);
    }

    public GetStationsQuery toQuery() {
        return new GetStationsQuery(page, size);
    }
}
