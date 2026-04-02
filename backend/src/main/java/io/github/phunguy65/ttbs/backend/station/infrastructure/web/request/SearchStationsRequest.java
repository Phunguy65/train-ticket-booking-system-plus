package io.github.phunguy65.ttbs.backend.station.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.station.application.query.SearchStationsQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record SearchStationsRequest(
        String q, @Min(1) @Max(20) int limit) {

    public SearchStationsRequest() {
        this(null, 10);
    }

    public SearchStationsQuery toQuery() {
        return new SearchStationsQuery(q, limit);
    }
}
