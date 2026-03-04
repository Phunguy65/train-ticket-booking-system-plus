package io.github.phunguy65.ttbs.backend.train.domain.event;

import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import java.time.Instant;
import java.util.List;

public record RoutesDeleted(List<RouteId> routeIds, Instant occurredAt) implements DomainEvent {

    public static RoutesDeleted of(List<RouteId> routeIds, Instant occurredAt) {
        return new RoutesDeleted(routeIds, occurredAt);
    }
}
