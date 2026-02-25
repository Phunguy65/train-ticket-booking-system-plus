package io.github.phunguy65.ttbs.backend.station.domain.event;

import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import java.time.Instant;

public record StationCreated(
        StationId stationId, String code, String name, String city, Instant occurredAt)
        implements DomainEvent {

    public static StationCreated of(StationId stationId, String code, String name, String city) {
        return new StationCreated(stationId, code, name, city, Instant.now());
    }
}
