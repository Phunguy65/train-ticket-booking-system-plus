package io.github.phunguy65.ttbs.backend.station.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.station.application.query.GetStationByIdQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "No additional query parameters are accepted when fetching a station.")
public record GetStationByIdRequest() {

    public GetStationByIdQuery toQuery(UUID stationId) {
        return new GetStationByIdQuery(stationId);
    }
}
