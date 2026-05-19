package io.github.phunguy65.ttbs.backend.train.domain.event;

import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import java.time.Instant;

public record SeatsCreated(CoachId coachId, int count, Instant occurredAt) implements DomainEvent {

    public static SeatsCreated of(CoachId coachId, int count) {
        return new SeatsCreated(coachId, count, Instant.now());
    }
}
