package io.github.phunguy65.ttbs.backend.station.application.command;

import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import java.util.List;

public record BulkSoftDeleteStationsCommand(List<StationId> stationIds) {}
