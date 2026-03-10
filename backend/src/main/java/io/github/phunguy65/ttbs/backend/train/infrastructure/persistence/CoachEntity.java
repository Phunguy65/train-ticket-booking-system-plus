package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "coaches")
class CoachEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "train_id", nullable = false, updatable = false)
    private UUID trainId;

    @Column(name = "car_number", nullable = false)
    private Integer carNumber;

    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected CoachEntity() {}

    UUID getId() {
        return id;
    }

    void setId(UUID id) {
        this.id = id;
    }

    UUID getTrainId() {
        return trainId;
    }

    void setTrainId(UUID trainId) {
        this.trainId = trainId;
    }

    Integer getCarNumber() {
        return carNumber;
    }

    void setCarNumber(Integer carNumber) {
        this.carNumber = carNumber;
    }

    Integer getTotalSeats() {
        return totalSeats;
    }

    void setTotalSeats(Integer totalSeats) {
        this.totalSeats = totalSeats;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    Instant getDeletedAt() {
        return deletedAt;
    }

    void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
