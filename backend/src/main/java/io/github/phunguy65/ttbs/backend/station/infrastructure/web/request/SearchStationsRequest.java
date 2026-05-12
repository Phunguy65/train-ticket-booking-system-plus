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
        @Min(1) @Max(20) Integer limit) {

    public SearchStationsRequest() {
        this(null, null);
    }

    public SearchStationsQuery toQuery() {
        return new SearchStationsQuery(q, limit != null ? limit : 10);
    }
}
