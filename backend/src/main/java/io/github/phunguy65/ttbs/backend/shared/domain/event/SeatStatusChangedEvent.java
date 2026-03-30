package io.github.phunguy65.ttbs.backend.shared.domain.event;

import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Application-level event broadcast to all SSE subscribers when seat statuses change after DB
 * commit.
 *
 * <p>Published via {@link org.springframework.transaction.event.TransactionPhase#AFTER_COMMIT}
 * to ensure clients only see committed state.
 *
 * <p>Located in {@code shared} so that {@code booking} and {@code payment} modules (which emit
 * the event) do not depend on the {@code seat} module (which consumes it). This avoids
 * cross-module coupling while keeping event-driven communication.
 */
public record SeatStatusChangedEvent(
        UUID scheduledTripId, List<SeatChange> seats, Instant occurredAt) implements DomainEvent {

    public record SeatChange(UUID seatId, String status, UUID bookingId) {}

    @Override
    public Instant occurredAt() {
        return occurredAt;
    }
}
