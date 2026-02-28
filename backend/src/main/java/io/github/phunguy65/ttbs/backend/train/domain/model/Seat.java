package io.github.phunguy65.ttbs.backend.train.domain.model;

import io.github.phunguy65.ttbs.backend.shared.domain.AggregateRoot;
import io.github.phunguy65.ttbs.backend.train.domain.event.SeatDeleted;
import java.time.Instant;

/**
 * Seat aggregate — represents a physical seat on a train.
 *
 * <p>Seats are administrative reference data. A new {@code Seat} does NOT emit domain events
 * at creation time (see design decision 6).
 */
public class Seat extends AggregateRoot<SeatId> {

    private final SeatId id;
    private final TrainId trainId;
    private final String seatNumber;
    private final Instant createdAt;
    private Instant deletedAt;

    private Seat(
            SeatId id, TrainId trainId, String seatNumber, Instant createdAt, Instant deletedAt) {
        this.id = id;
        this.trainId = trainId;
        this.seatNumber = seatNumber;
        this.createdAt = createdAt;
        this.deletedAt = deletedAt;
    }

    /**
     * Factory method for creating a new seat.
     * Does NOT register domain events (seats are admin reference data).
     */
    public static Seat create(SeatId id, TrainId trainId, String seatNumber) {
        return new Seat(id, trainId, seatNumber, Instant.now(), null);
    }

    /**
     * Factory method for reconstituting a seat from persistence.
     * Does NOT register domain events.
     */
    public static Seat reconstitute(
            SeatId id, TrainId trainId, String seatNumber, Instant createdAt, Instant deletedAt) {
        return new Seat(id, trainId, seatNumber, createdAt, deletedAt);
    }

    /**
     * Soft-deletes this seat by setting {@code deletedAt} to now and registering a
     * {@link SeatDeleted} domain event. Idempotent: if already deleted, returns immediately.
     */
    public void softDelete() {
        if (isDeleted()) {
            return;
        }
        this.deletedAt = Instant.now();
        registerEvent(SeatDeleted.of(id));
    }

    /** Returns {@code true} if this seat has been soft-deleted. */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    @Override
    public SeatId getId() {
        return id;
    }

    public TrainId getTrainId() {
        return trainId;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
