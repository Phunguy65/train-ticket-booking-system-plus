package io.github.phunguy65.ttbs.backend.station.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.station.application.query.SearchStationsQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "Station auto-complete query.")
public record SearchStationsRequest(
        @Schema(description = "Free-text station search term.", example = "ha noi")
        String q,

        @Schema(
                description = "Maximum number of station suggestions to return.",
                minimum = "1",
                maximum = "20",
                example = "10")
        @Min(1) @Max(20) int limit) {

    public SearchStationsRequest() {
        this(null, 10);
    }

    public SearchStationsQuery toQuery() {
        return new SearchStationsQuery(q, limit);
    }
}
