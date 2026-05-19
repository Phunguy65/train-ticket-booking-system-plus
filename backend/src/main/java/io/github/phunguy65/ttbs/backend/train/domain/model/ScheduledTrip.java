package io.github.phunguy65.ttbs.backend.train.domain.model;

import io.github.phunguy65.ttbs.backend.shared.domain.AggregateRoot;
import java.time.Instant;
import java.util.Objects;

/**
 * Concrete departure instance that can be sold and optionally assigned to a physical train.
 */
public class ScheduledTrip extends AggregateRoot<ScheduledTripId> {

    private final ScheduledTripId id;
    private final RouteTemplateId routeTemplateId;
    private TrainId trainId;
    private Instant departureTime;
    private Instant arrivalTime;
    private ScheduledTripStatus status;
    private final Instant createdAt;
    private Instant deletedAt;

    private ScheduledTrip(
            ScheduledTripId id,
            RouteTemplateId routeTemplateId,
            TrainId trainId,
            Instant departureTime,
            Instant arrivalTime,
            ScheduledTripStatus status,
            Instant createdAt,
            Instant deletedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.routeTemplateId =
                Objects.requireNonNull(routeTemplateId, "routeTemplateId must not be null");
        this.trainId = trainId;
        this.departureTime =
                Objects.requireNonNull(departureTime, "departureTime must not be null");
        this.arrivalTime = Objects.requireNonNull(arrivalTime, "arrivalTime must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.deletedAt = deletedAt;
    }

    public static ScheduledTrip create(
            ScheduledTripId id,
            RouteTemplateId routeTemplateId,
            TrainId trainId,
            Instant departureTime,
            Instant arrivalTime) {
        validateSchedule(departureTime, arrivalTime);
        return new ScheduledTrip(
                id,
                routeTemplateId,
                trainId,
                departureTime,
                arrivalTime,
                ScheduledTripStatus.SCHEDULED,
                Instant.now(),
                null);
    }

    public static ScheduledTrip reconstitute(
            ScheduledTripId id,
            RouteTemplateId routeTemplateId,
            TrainId trainId,
            Instant departureTime,
            Instant arrivalTime,
            ScheduledTripStatus status,
            Instant createdAt,
            Instant deletedAt) {
        validateSchedule(departureTime, arrivalTime);
        return new ScheduledTrip(
                id,
                routeTemplateId,
                trainId,
                departureTime,
                arrivalTime,
                status,
                createdAt,
                deletedAt);
    }

    public void assignTrain(TrainId trainId) {
        this.trainId = trainId;
    }

    public void unassignTrain() {
        this.trainId = null;
    }

    public void reschedule(Instant departureTime, Instant arrivalTime) {
        validateSchedule(departureTime, arrivalTime);
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
    }

    public void updateStatus(ScheduledTripStatus status) {
        this.status = Objects.requireNonNull(status, "status must not be null");
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
    public ScheduledTripId getId() {
        return id;
    }

    public RouteTemplateId getRouteTemplateId() {
        return routeTemplateId;
    }

    public TrainId getTrainId() {
        return trainId;
    }

    public Instant getDepartureTime() {
        return departureTime;
    }

    public Instant getArrivalTime() {
        return arrivalTime;
    }

    public ScheduledTripStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    private static void validateSchedule(Instant departureTime, Instant arrivalTime) {
        Objects.requireNonNull(departureTime, "departureTime must not be null");
        Objects.requireNonNull(arrivalTime, "arrivalTime must not be null");
        if (!arrivalTime.isAfter(departureTime)) {
            throw new IllegalArgumentException("arrivalTime must be after departureTime");
        }
    }
}
