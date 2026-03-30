package io.github.phunguy65.ttbs.backend.station.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.station.application.query.GetStationByIdQuery;
import java.util.UUID;

public record GetStationByIdRequest() {

    public GetStationByIdQuery toQuery(UUID stationId) {
        return new GetStationByIdQuery(stationId);
    }
}
