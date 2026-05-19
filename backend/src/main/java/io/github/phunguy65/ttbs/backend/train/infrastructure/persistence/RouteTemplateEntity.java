package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "route_templates")
class RouteTemplateEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "origin_station_id", nullable = false)
    private UUID originStationId;

    @Column(name = "destination_station_id", nullable = false)
    private UUID destinationStationId;

    @Column(name = "base_price", nullable = false)
    private long basePrice;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected RouteTemplateEntity() {}

    UUID getId() {
        return id;
    }

    void setId(UUID id) {
        this.id = id;
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

    long getBasePrice() {
        return basePrice;
    }

    void setBasePrice(long basePrice) {
        this.basePrice = basePrice;
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
