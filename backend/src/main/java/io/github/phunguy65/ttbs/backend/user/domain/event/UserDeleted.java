package io.github.phunguy65.ttbs.backend.user.domain.event;

import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.time.Instant;

public record UserDeleted(UserId userId, Instant occurredAt) implements DomainEvent {

    public static UserDeleted of(UserId userId) {
        return new UserDeleted(userId, Instant.now());
    }
}
