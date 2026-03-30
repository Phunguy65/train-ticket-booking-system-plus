package io.github.phunguy65.ttbs.backend.train.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.request.PagedRequest;
import io.github.phunguy65.ttbs.backend.train.application.query.GetScheduledTripsQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record GetScheduledTripsRequest(
        @Min(0) int page, @Min(1) @Max(100) int size) implements PagedRequest {

    public GetScheduledTripsRequest() {
        this(0, 20);
    }

    public GetScheduledTripsQuery toQuery() {
        return new GetScheduledTripsQuery(page, size);
    }
}
