package io.github.phunguy65.ttbs.backend.train.domain.model;

import io.github.phunguy65.ttbs.backend.shared.domain.AggregateRoot;
import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import java.time.Instant;
import java.util.Objects;

/**
 * Reusable journey definition shared by many scheduled trips over time.
 */
public class RouteTemplate extends AggregateRoot<RouteTemplateId> {

    private final RouteTemplateId id;
    private StationId originStationId;
    private StationId destinationStationId;
    private Money basePrice;
    private final Instant createdAt;
    private Instant deletedAt;

    private RouteTemplate(
            RouteTemplateId id,
            StationId originStationId,
            StationId destinationStationId,
            Money basePrice,
            Instant createdAt,
            Instant deletedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.originStationId =
                Objects.requireNonNull(originStationId, "originStationId must not be null");
        this.destinationStationId = Objects.requireNonNull(
                destinationStationId, "destinationStationId must not be null");
        this.basePrice = Objects.requireNonNull(basePrice, "basePrice must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.deletedAt = deletedAt;
    }

    public static RouteTemplate create(
            RouteTemplateId id,
            StationId originStationId,
            StationId destinationStationId,
            Money basePrice) {
        return new RouteTemplate(
                id, originStationId, destinationStationId, basePrice, Instant.now(), null);
    }

    public static RouteTemplate reconstitute(
            RouteTemplateId id,
            StationId originStationId,
            StationId destinationStationId,
            Money basePrice,
            Instant createdAt,
            Instant deletedAt) {
        return new RouteTemplate(
                id, originStationId, destinationStationId, basePrice, createdAt, deletedAt);
    }

    public void update(StationId originStationId, StationId destinationStationId, Money basePrice) {
        this.originStationId =
                Objects.requireNonNull(originStationId, "originStationId must not be null");
        this.destinationStationId = Objects.requireNonNull(
                destinationStationId, "destinationStationId must not be null");
        this.basePrice = Objects.requireNonNull(basePrice, "basePrice must not be null");
    }

    public void softDelete() {
        if (isDeleted()) {
            return;
        }
        this.deletedAt = Instant.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    @Override
    public RouteTemplateId getId() {
        return id;
    }

    public StationId getOriginStationId() {
        return originStationId;
    }

    public StationId getDestinationStationId() {
        return destinationStationId;
    }

    public Money getBasePrice() {
        return basePrice;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
