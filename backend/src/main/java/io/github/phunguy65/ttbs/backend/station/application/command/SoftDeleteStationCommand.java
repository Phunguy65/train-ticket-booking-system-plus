package io.github.phunguy65.ttbs.backend.station.application.command;

import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;

public record SoftDeleteStationCommand(StationId stationId) {}
