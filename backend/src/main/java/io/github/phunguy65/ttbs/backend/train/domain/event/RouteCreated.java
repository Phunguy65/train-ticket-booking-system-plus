package io.github.phunguy65.ttbs.backend.train.domain.event;

import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import java.time.Instant;

/**
 * Domain event published when a new route is created.
 *
 * <p>Consumed by {@link io.github.phunguy65.ttbs.backend.train.application.listener.SeatAvailabilitySeeder}
 * to pre-populate {@code route_seat_availability} rows for all seats on the route's train.
 */
public record RouteCreated(RouteId routeId, TrainId trainId, Instant occurredAt)
        implements DomainEvent {

    public static RouteCreated of(RouteId routeId, TrainId trainId) {
        return new RouteCreated(routeId, trainId, Instant.now());
    }
}
