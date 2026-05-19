package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "scheduled_trips")
class ScheduledTripEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "route_template_id", nullable = false, updatable = false)
    private UUID routeTemplateId;

    @Column(name = "train_id")
    private UUID trainId;

    @Column(name = "departure_time", nullable = false)
    private Instant departureTime;

    @Column(name = "arrival_time", nullable = false)
    private Instant arrivalTime;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected ScheduledTripEntity() {}

    UUID getId() {
        return id;
    }

    void setId(UUID id) {
        this.id = id;
    }

    UUID getRouteTemplateId() {
        return routeTemplateId;
    }

    void setRouteTemplateId(UUID routeTemplateId) {
        this.routeTemplateId = routeTemplateId;
    }

    UUID getTrainId() {
        return trainId;
    }

    void setTrainId(UUID trainId) {
        this.trainId = trainId;
    }

    Instant getDepartureTime() {
        return departureTime;
    }

    void setDepartureTime(Instant departureTime) {
        this.departureTime = departureTime;
    }

    Instant getArrivalTime() {
        return arrivalTime;
    }

    void setArrivalTime(Instant arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    String getStatus() {
        return status;
    }

    void setStatus(String status) {
        this.status = status;
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
