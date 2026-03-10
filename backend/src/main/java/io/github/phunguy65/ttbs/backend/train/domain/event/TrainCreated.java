package io.github.phunguy65.ttbs.backend.train.domain.event;

import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import java.time.Instant;

public record TrainCreated(TrainId trainId, String trainNumber, Instant occurredAt)
        implements DomainEvent {

    public static TrainCreated of(TrainId trainId, String trainNumber) {
        return new TrainCreated(trainId, trainNumber, Instant.now());
    }
}
