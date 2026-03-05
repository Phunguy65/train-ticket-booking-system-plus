package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "routes")
class RouteEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "train_id", nullable = false, updatable = false)
    private UUID trainId;

    @Column(name = "origin_station_id", nullable = false, updatable = false)
    private UUID originStationId;

    @Column(name = "destination_station_id", nullable = false, updatable = false)
    private UUID destinationStationId;

    @Column(name = "departure_time", nullable = false)
    private Instant departureTime;

    @Column(name = "arrival_time", nullable = false)
    private Instant arrivalTime;

    @Column(name = "base_price", nullable = false)
    private long basePrice;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected RouteEntity() {}

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

    UUID getOriginStationId() {
        return originStationId;
    }

    void setOriginStationId(UUID originStationId) {
        this.originStationId = originStationId;
    }

    UUID getDestinationStationId() {
        return destinationStationId;
    }

    void setDestinationStationId(UUID destinationStationId) {
        this.destinationStationId = destinationStationId;
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

    long getBasePrice() {
        return basePrice;
    }

    void setBasePrice(long basePrice) {
        this.basePrice = basePrice;
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
