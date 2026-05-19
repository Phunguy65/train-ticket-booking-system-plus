package io.github.phunguy65.ttbs.backend.train.domain.event;

import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import java.time.Instant;

public record SeatCreated(SeatId seatId, CoachId coachId, Instant occurredAt)
        implements DomainEvent {

    public static SeatCreated of(SeatId seatId, CoachId coachId) {
        return new SeatCreated(seatId, coachId, Instant.now());
    }
}
