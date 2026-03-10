package io.github.phunguy65.ttbs.backend.train.domain.model;

import io.github.phunguy65.ttbs.backend.shared.domain.AggregateRoot;
import io.github.phunguy65.ttbs.backend.train.domain.event.TrainCreated;
import io.github.phunguy65.ttbs.backend.train.domain.event.TrainDeleted;
import java.time.Instant;

public class Train extends AggregateRoot<TrainId> {

    private final TrainId id;
    private final String trainNumber;
    private final String name;
    private final int totalSeats;
    private final Instant createdAt;
    private Instant deletedAt;

    private Train(
            TrainId id,
            String trainNumber,
            String name,
            int totalSeats,
            Instant createdAt,
            Instant deletedAt) {
        this.id = id;
        this.trainNumber = trainNumber;
        this.name = name;
        this.totalSeats = totalSeats;
        this.createdAt = createdAt;
        this.deletedAt = deletedAt;
    }

    /**
     * Factory method for creating a new train. Registers {@link TrainCreated} domain event.
     */
    public static Train create(TrainId id, String trainNumber, String name, int totalSeats) {
        Instant now = Instant.now();
        Train train = new Train(id, trainNumber, name, totalSeats, now, null);
        train.registerEvent(TrainCreated.of(id, trainNumber));
        return train;
    }

    /**
     * Factory method for reconstituting a train from persistence.
     * Does NOT register domain events.
     */
    public static Train reconstitute(
            TrainId id,
            String trainNumber,
            String name,
            int totalSeats,
            Instant createdAt,
            Instant deletedAt) {
        return new Train(id, trainNumber, name, totalSeats, createdAt, deletedAt);
    }

    /**
     * Soft-deletes this train by setting {@code deletedAt} to now and registering a
     * {@link TrainDeleted} domain event. Idempotent: if already deleted, returns immediately.
     */
    public void softDelete() {
        if (isDeleted()) {
            return;
        }
        this.deletedAt = Instant.now();
        registerEvent(TrainDeleted.of(id));
    }

    /** Returns {@code true} if this train has been soft-deleted. */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    @Override
    public TrainId getId() {
        return id;
    }

    public String getTrainNumber() {
        return trainNumber;
    }

    public String getName() {
        return name;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
