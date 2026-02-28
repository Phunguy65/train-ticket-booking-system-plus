package io.github.phunguy65.ttbs.backend.train.domain.event;

import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import java.time.Instant;

public record CoachDeleted(CoachId coachId, Instant occurredAt) implements DomainEvent {

    public static CoachDeleted of(CoachId coachId) {
        return new CoachDeleted(coachId, Instant.now());
    }
}
