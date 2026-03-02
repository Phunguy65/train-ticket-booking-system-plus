package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trains")
class TrainEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "train_number", nullable = false, unique = true, length = 20)
    private String trainNumber;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "total_seats", nullable = false)
    private int totalSeats;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected TrainEntity() {}

    UUID getId() {
        return id;
    }

    void setId(UUID id) {
        this.id = id;
    }

    String getTrainNumber() {
        return trainNumber;
    }

    void setTrainNumber(String trainNumber) {
        this.trainNumber = trainNumber;
    }

    String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    int getTotalSeats() {
        return totalSeats;
    }

    void setTotalSeats(int totalSeats) {
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
