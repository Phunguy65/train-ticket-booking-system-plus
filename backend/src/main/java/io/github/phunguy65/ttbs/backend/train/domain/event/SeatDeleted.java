package io.github.phunguy65.ttbs.backend.train.domain.event;

import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import java.time.Instant;

public record SeatDeleted(SeatId seatId, Instant occurredAt) implements DomainEvent {

    public static SeatDeleted of(SeatId seatId) {
        return new SeatDeleted(seatId, Instant.now());
    }
}
