package io.github.phunguy65.ttbs.backend.train.domain.event;

import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteStatus;
import java.time.Instant;

public record RouteUpdated(RouteId routeId, RouteStatus status, Instant occurredAt)
        implements DomainEvent {

    public static RouteUpdated of(RouteId routeId, RouteStatus status) {
        return new RouteUpdated(routeId, status, Instant.now());
    }
}
