package io.github.phunguy65.ttbs.backend.station.domain.event;

import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import java.time.Instant;

public record StationDeleted(StationId stationId, Instant occurredAt) implements DomainEvent {

    public static StationDeleted of(StationId stationId) {
        return new StationDeleted(stationId, Instant.now());
    }
}
