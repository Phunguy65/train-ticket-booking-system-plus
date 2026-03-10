package io.github.phunguy65.ttbs.backend.train.domain.event;

import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import java.time.Instant;

public record TrainDeleted(TrainId trainId, Instant occurredAt) implements DomainEvent {

    public static TrainDeleted of(TrainId trainId) {
        return new TrainDeleted(trainId, Instant.now());
    }
}
