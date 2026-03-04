package io.github.phunguy65.ttbs.backend.station.domain.event;

import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import java.time.Instant;
import java.util.List;

public record StationsDeleted(List<StationId> stationIds, Instant occurredAt)
        implements DomainEvent {

    public static StationsDeleted of(List<StationId> stationIds, Instant occurredAt) {
        return new StationsDeleted(stationIds, occurredAt);
    }
}
