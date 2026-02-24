package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
class RouteSeatAvailabilityId implements Serializable {

    @Column(name = "route_id", nullable = false, updatable = false)
    private UUID routeId;

    @Column(name = "seat_id", nullable = false, updatable = false)
    private UUID seatId;

    protected RouteSeatAvailabilityId() {}

    RouteSeatAvailabilityId(UUID routeId, UUID seatId) {
        this.routeId = routeId;
        this.seatId = seatId;
    }

    UUID getRouteId() {
        return routeId;
    }

    UUID getSeatId() {
        return seatId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RouteSeatAvailabilityId that)) return false;
        return Objects.equals(routeId, that.routeId) && Objects.equals(seatId, that.seatId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(routeId, seatId);
    }
}
