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

    @Column(name = "coach_id", nullable = false, updatable = false)
    private UUID coachId;

    @Column(name = "seat_number", nullable = false, length = 10)
    private String seatNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected SeatEntity() {}

    UUID getId() {
        return id;
    }

    void setId(UUID id) {
        this.id = id;
    }

    UUID getCoachId() {
        return coachId;
    }

    void setCoachId(UUID coachId) {
        this.coachId = coachId;
    }

    String getSeatNumber() {
        return seatNumber;
    }

    void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
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
