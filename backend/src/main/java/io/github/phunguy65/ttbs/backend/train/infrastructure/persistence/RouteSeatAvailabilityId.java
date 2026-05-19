package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
class RouteSeatAvailabilityId implements Serializable {

    @Column(name = "scheduled_trip_id", nullable = false, updatable = false)
    private UUID scheduledTripId;

    @Column(name = "seat_id", nullable = false, updatable = false)
    private UUID seatId;

    protected RouteSeatAvailabilityId() {}

    RouteSeatAvailabilityId(UUID scheduledTripId, UUID seatId) {
        this.scheduledTripId = scheduledTripId;
        this.seatId = seatId;
    }

    UUID getScheduledTripId() {
        return scheduledTripId;
    }

    UUID getSeatId() {
        return seatId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RouteSeatAvailabilityId that)) return false;
        return Objects.equals(scheduledTripId, that.scheduledTripId)
                && Objects.equals(seatId, that.seatId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scheduledTripId, seatId);
    }
}
