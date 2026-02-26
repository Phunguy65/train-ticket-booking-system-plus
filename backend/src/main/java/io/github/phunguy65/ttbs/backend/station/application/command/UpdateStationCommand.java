package io.github.phunguy65.ttbs.backend.station.application.command;

import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateStationCommand(
        StationId stationId,
        JsonNullable<String> code,
        JsonNullable<String> name,
        JsonNullable<String> city) {}
