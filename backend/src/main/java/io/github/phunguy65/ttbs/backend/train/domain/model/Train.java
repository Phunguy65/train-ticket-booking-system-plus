package io.github.phunguy65.ttbs.backend.train.domain.model;

import io.github.phunguy65.ttbs.backend.shared.domain.AggregateRoot;
import io.github.phunguy65.ttbs.backend.train.domain.event.TrainCreated;
import io.github.phunguy65.ttbs.backend.train.domain.event.TrainDeleted;
import io.github.phunguy65.ttbs.backend.train.domain.event.TrainUpdated;
import java.time.Instant;

public class Train extends AggregateRoot<TrainId> {

    private final TrainId id;
    private TrainNumber trainNumber;
    private String name;
    private int totalSeats;
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
        this.trainNumber = TrainNumber.of(trainNumber);
        this.name = name;
        this.totalSeats = totalSeats;
        this.createdAt = createdAt;
        this.deletedAt = deletedAt;
    }

    /**
     * Factory method for creating a new train. Registers {@link TrainCreated} domain event.
     * {@code totalSeats} starts at 0 and is updated automatically as seats are created/deleted.
     */
    public static Train create(TrainId id, String trainNumber, String name) {
        Instant now = Instant.now();
        Train train = new Train(id, trainNumber, name, 0, now, null);
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
     * Updates mutable fields of this train and registers a {@link TrainUpdated} domain event.
     *
     * @param trainNumber new train number (must not be null)
     * @param name        new display name (must not be null)
     */
    public void update(String trainNumber, String name) {
        this.trainNumber = TrainNumber.of(trainNumber);
        this.name = name;
        registerEvent(TrainUpdated.of(id, trainNumber));
    }

    /**
     * Updates the total seat count. Called by event listeners when seats are created or removed.
     * {@code totalSeats} must be non-negative (0 is allowed when all seats are removed).
     *
     * @param count new total seat count
     */
    public void updateTotalSeats(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("totalSeats must be non-negative");
        }
        this.totalSeats = count;
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
        return trainNumber.value();
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
