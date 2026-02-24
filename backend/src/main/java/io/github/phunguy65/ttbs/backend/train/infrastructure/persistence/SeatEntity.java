package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "seats")
class SeatEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "train_id", nullable = false, updatable = false)
    private UUID trainId;

    @Column(name = "seat_number", nullable = false)
    private String seatNumber;

    @Column(name = "seat_class", nullable = false)
    private String seatClass;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SeatEntity() {}

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

    String getSeatNumber() {
        return seatNumber;
    }

    void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    String getSeatClass() {
        return seatClass;
    }

    void setSeatClass(String seatClass) {
        this.seatClass = seatClass;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
