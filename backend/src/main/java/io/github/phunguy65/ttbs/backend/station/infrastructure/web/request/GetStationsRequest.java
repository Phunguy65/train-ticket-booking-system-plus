package io.github.phunguy65.ttbs.backend.station.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.station.application.query.GetStationsQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record GetStationsRequest(
        @Min(0) int page, @Min(1) @Max(100) int size) {

    public GetStationsRequest() {
        this(0, 20);
    }

    public GetStationsQuery toQuery() {
        return new GetStationsQuery(page, size);
    }
}
