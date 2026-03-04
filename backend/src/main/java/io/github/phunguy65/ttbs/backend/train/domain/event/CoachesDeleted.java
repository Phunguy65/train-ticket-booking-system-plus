package io.github.phunguy65.ttbs.backend.train.domain.event;

import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import java.time.Instant;
import java.util.List;

public record CoachesDeleted(List<CoachId> coachIds, Instant occurredAt) implements DomainEvent {

    public static CoachesDeleted of(List<CoachId> coachIds, Instant occurredAt) {
        return new CoachesDeleted(coachIds, occurredAt);
    }
}
