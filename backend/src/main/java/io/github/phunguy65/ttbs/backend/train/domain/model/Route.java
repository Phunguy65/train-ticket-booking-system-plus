package io.github.phunguy65.ttbs.backend.train.domain.model;

import io.github.phunguy65.ttbs.backend.shared.domain.AggregateRoot;
import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import io.github.phunguy65.ttbs.backend.train.domain.event.RouteCreated;
import io.github.phunguy65.ttbs.backend.train.domain.event.RouteDeleted;
import java.time.Instant;

/**
 * The Route aggregate root — represents a specific scheduled train trip from an origin station to
 * a destination station at a particular departure time.
 *
 * <p>Use {@link #create} to produce a new route (registers {@link RouteCreated} domain event).
 * Use {@link #reconstitute} when loading from persistence (no events registered).
 */
public class Route extends AggregateRoot<RouteId> {

    private final RouteId id;
    private final TrainId trainId;
    private final StationId originStationId;
    private final StationId destinationStationId;
    private final Instant departureTime;
    private final Instant arrivalTime;
    private final Money basePrice;
    private final RouteStatus status;
    private final Instant createdAt;
    private Instant deletedAt;

    private Route(
            RouteId id,
            TrainId trainId,
            StationId originStationId,
            StationId destinationStationId,
            Instant departureTime,
            Instant arrivalTime,
            Money basePrice,
            RouteStatus status,
            Instant createdAt,
            Instant deletedAt) {
        this.id = id;
        this.trainId = trainId;
        this.originStationId = originStationId;
        this.destinationStationId = destinationStationId;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.basePrice = basePrice;
        this.status = status;
        this.createdAt = createdAt;
        this.deletedAt = deletedAt;
    }

    /**
     * Factory method for creating a new route.
     *
     * <p>Validates that {@code arrivalTime} is strictly after {@code departureTime}, then
     * registers a {@link RouteCreated} domain event.
     *
     * @throws IllegalArgumentException if {@code arrivalTime} is not after {@code departureTime}
     */
    public static Route create(
            RouteId id,
            TrainId trainId,
            StationId originStationId,
            StationId destinationStationId,
            Instant departureTime,
            Instant arrivalTime,
            Money basePrice) {
        if (!arrivalTime.isAfter(departureTime)) {
            throw new IllegalArgumentException("arrivalTime must be after departureTime");
        }
        Instant now = Instant.now();
        Route route = new Route(
                id,
                trainId,
                originStationId,
                destinationStationId,
                departureTime,
                arrivalTime,
                basePrice,
                RouteStatus.SCHEDULED,
                now,
                null);
        route.registerEvent(RouteCreated.of(id, trainId));
        return route;
    }

    /**
     * Factory method for reconstituting a route from persistence.
     * Does NOT register domain events.
     */
    public static Route reconstitute(
            RouteId id,
            TrainId trainId,
            StationId originStationId,
            StationId destinationStationId,
            Instant departureTime,
            Instant arrivalTime,
            Money basePrice,
            RouteStatus status,
            Instant createdAt,
            Instant deletedAt) {
        return new Route(
                id,
                trainId,
                originStationId,
                destinationStationId,
                departureTime,
                arrivalTime,
                basePrice,
                status,
                createdAt,
                deletedAt);
    }

    /**
     * Soft-deletes this route by setting {@code deletedAt} to now and registering a
     * {@link RouteDeleted} domain event. Idempotent: if already deleted, returns immediately.
     */
    public void softDelete() {
        if (isDeleted()) {
            return;
        }
        this.deletedAt = Instant.now();
        registerEvent(RouteDeleted.of(id));
    }

    /** Returns {@code true} if this route has been soft-deleted. */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    @Override
    public RouteId getId() {
        return id;
    }

    public TrainId getTrainId() {
        return trainId;
    }

    public StationId getOriginStationId() {
        return originStationId;
    }

    public StationId getDestinationStationId() {
        return destinationStationId;
    }

    public Instant getDepartureTime() {
        return departureTime;
    }

    public Instant getArrivalTime() {
        return arrivalTime;
    }

    public Money getBasePrice() {
        return basePrice;
    }

    public RouteStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
