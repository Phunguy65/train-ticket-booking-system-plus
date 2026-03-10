package io.github.phunguy65.ttbs.backend.train.domain.event;

import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import java.time.Instant;
import java.util.List;

public record TrainsDeleted(List<TrainId> trainIds, Instant occurredAt) implements DomainEvent {

    public static TrainsDeleted of(List<TrainId> trainIds, Instant occurredAt) {
        return new TrainsDeleted(trainIds, occurredAt);
    }
}
