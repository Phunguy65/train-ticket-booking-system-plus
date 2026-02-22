package io.github.phunguy65.ttbs.backend.user.domain.event;

import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.shared.domain.UserId;
import java.time.Instant;

public record UserRegistered(UserId userId, String email, Instant occurredAt)
        implements DomainEvent {

    public static UserRegistered of(UserId userId, String email) {
        return new UserRegistered(userId, email, Instant.now());
    }
}
