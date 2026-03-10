package io.github.phunguy65.ttbs.backend.train.domain.model;

import io.github.phunguy65.ttbs.backend.shared.domain.AggregateRoot;
import io.github.phunguy65.ttbs.backend.train.domain.event.CoachDeleted;
import java.time.Instant;

/**
 * Coach aggregate — represents a physical train car (toa tàu).
 *
 * <p>Coaches are administrative reference data. A new {@code Coach} does NOT emit domain events
 * at creation time (see design decision 2).
 */
public class Coach extends AggregateRoot<CoachId> {

    private final CoachId id;
    private final TrainId trainId;
    private final int carNumber;
    private final int totalSeats;
    private final Instant createdAt;
    private Instant deletedAt;

    private Coach(
            CoachId id,
            TrainId trainId,
            int carNumber,
            int totalSeats,
            Instant createdAt,
            Instant deletedAt) {
        this.id = id;
        this.trainId = trainId;
        this.carNumber = carNumber;
        this.totalSeats = totalSeats;
        this.createdAt = createdAt;
        this.deletedAt = deletedAt;
    }

    /**
     * Factory method for creating a new coach.
     * Does NOT register domain events (coaches are admin reference data).
     */
    public static Coach create(CoachId id, TrainId trainId, int carNumber, int totalSeats) {
        return new Coach(id, trainId, carNumber, totalSeats, Instant.now(), null);
    }

    /**
     * Factory method for reconstituting a coach from persistence.
     * Does NOT register domain events.
     */
    public static Coach reconstitute(
            CoachId id,
            TrainId trainId,
            int carNumber,
            int totalSeats,
            Instant createdAt,
            Instant deletedAt) {
        return new Coach(id, trainId, carNumber, totalSeats, createdAt, deletedAt);
    }

    /**
     * Soft-deletes this coach by setting {@code deletedAt} to now and registering a
     * {@link CoachDeleted} domain event. Idempotent: if already deleted, returns immediately.
     */
    public void softDelete() {
        if (isDeleted()) {
            return;
        }
        this.deletedAt = Instant.now();
        registerEvent(CoachDeleted.of(id));
    }

    /** Returns {@code true} if this coach has been soft-deleted. */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    @Override
    public CoachId getId() {
        return id;
    }

    public TrainId getTrainId() {
        return trainId;
    }

    public int getCarNumber() {
        return carNumber;
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
