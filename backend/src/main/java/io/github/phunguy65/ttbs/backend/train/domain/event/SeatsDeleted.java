package io.github.phunguy65.ttbs.backend.train.domain.event;

import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import java.time.Instant;
import java.util.List;

public record SeatsDeleted(List<SeatId> seatIds, Instant occurredAt) implements DomainEvent {

    public static SeatsDeleted of(List<SeatId> seatIds, Instant occurredAt) {
        return new SeatsDeleted(seatIds, occurredAt);
    }
}
