package io.github.phunguy65.ttbs.backend.train.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ScheduledTripTest {

    @Test
    void createInitializesScheduledTrip() {
        ScheduledTripId id = ScheduledTripId.of(UUID.randomUUID());
        RouteTemplateId routeTemplateId = RouteTemplateId.of(UUID.randomUUID());
        TrainId trainId = TrainId.of(UUID.randomUUID());
        Instant departureTime = Instant.parse("2026-03-27T06:00:00Z");
        Instant arrivalTime = Instant.parse("2026-03-27T08:00:00Z");

        ScheduledTrip trip =
                ScheduledTrip.create(id, routeTemplateId, trainId, departureTime, arrivalTime);

        assertThat(trip.getId()).isEqualTo(id);
        assertThat(trip.getRouteTemplateId()).isEqualTo(routeTemplateId);
        assertThat(trip.getTrainId()).isEqualTo(trainId);
        assertThat(trip.getDepartureTime()).isEqualTo(departureTime);
        assertThat(trip.getArrivalTime()).isEqualTo(arrivalTime);
        assertThat(trip.getStatus()).isEqualTo(ScheduledTripStatus.SCHEDULED);
        assertThat(trip.getCreatedAt()).isNotNull();
        assertThat(trip.getDeletedAt()).isNull();
    }

    @Test
    void createAllowsMissingTrainAssignment() {
        ScheduledTrip trip = ScheduledTrip.create(
                ScheduledTripId.of(UUID.randomUUID()),
                RouteTemplateId.of(UUID.randomUUID()),
                null,
                Instant.parse("2026-03-27T06:00:00Z"),
                Instant.parse("2026-03-27T08:00:00Z"));

        assertThat(trip.getTrainId()).isNull();
    }

    @Test
    void createRejectsNullRouteTemplateId() {
        assertThatThrownBy(() -> ScheduledTrip.create(
                        ScheduledTripId.of(UUID.randomUUID()),
                        null,
                        null,
                        Instant.parse("2026-03-27T06:00:00Z"),
                        Instant.parse("2026-03-27T08:00:00Z")))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("routeTemplateId must not be null");
    }

    @Test
    void createRejectsArrivalBeforeDeparture() {
        assertThatThrownBy(() -> ScheduledTrip.create(
                        ScheduledTripId.of(UUID.randomUUID()),
                        RouteTemplateId.of(UUID.randomUUID()),
                        null,
                        Instant.parse("2026-03-27T08:00:00Z"),
                        Instant.parse("2026-03-27T06:00:00Z")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("arrivalTime must be after departureTime");
    }

    @Test
    void createRejectsArrivalEqualToDeparture() {
        Instant sameTime = Instant.parse("2026-03-27T06:00:00Z");

        assertThatThrownBy(() -> ScheduledTrip.create(
                        ScheduledTripId.of(UUID.randomUUID()),
                        RouteTemplateId.of(UUID.randomUUID()),
                        null,
                        sameTime,
                        sameTime))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("arrivalTime must be after departureTime");
    }

    @Test
    void createRejectsNullDepartureTime() {
        assertThatThrownBy(() -> ScheduledTrip.create(
                        ScheduledTripId.of(UUID.randomUUID()),
                        RouteTemplateId.of(UUID.randomUUID()),
                        null,
                        null,
                        Instant.parse("2026-03-27T08:00:00Z")))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("departureTime must not be null");
    }

    @Test
    void createRejectsNullArrivalTime() {
        assertThatThrownBy(() -> ScheduledTrip.create(
                        ScheduledTripId.of(UUID.randomUUID()),
                        RouteTemplateId.of(UUID.randomUUID()),
                        null,
                        Instant.parse("2026-03-27T06:00:00Z"),
                        null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("arrivalTime must not be null");
    }

    @Test
    void assignAndUnassignTrainUpdateAssignment() {
        ScheduledTrip trip = createTrip();
        TrainId newTrainId = TrainId.of(UUID.randomUUID());

        trip.assignTrain(newTrainId);
        assertThat(trip.getTrainId()).isEqualTo(newTrainId);

        trip.unassignTrain();
        assertThat(trip.getTrainId()).isNull();
    }

    @Test
    void rescheduleUpdatesTimesWhenValid() {
        ScheduledTrip trip = createTrip();
        Instant newDeparture = Instant.parse("2026-03-28T06:30:00Z");
        Instant newArrival = Instant.parse("2026-03-28T08:45:00Z");

        trip.reschedule(newDeparture, newArrival);

        assertThat(trip.getDepartureTime()).isEqualTo(newDeparture);
        assertThat(trip.getArrivalTime()).isEqualTo(newArrival);
    }

    @Test
    void rescheduleRejectsInvalidTimes() {
        ScheduledTrip trip = createTrip();

        assertThatThrownBy(() -> trip.reschedule(
                        Instant.parse("2026-03-28T09:00:00Z"),
                        Instant.parse("2026-03-28T08:45:00Z")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("arrivalTime must be after departureTime");
    }

    @Test
    void updateStatusAllowsOperationalChanges() {
        ScheduledTrip trip = createTrip();

        for (ScheduledTripStatus status : ScheduledTripStatus.values()) {
            trip.updateStatus(status);
            assertThat(trip.getStatus()).isEqualTo(status);
        }
    }

    @Test
    void softDeleteIsIdempotent() {
        ScheduledTrip trip = createTrip();

        trip.softDelete();
        Instant firstDeletedAt = trip.getDeletedAt();
        trip.softDelete();

        assertThat(trip.isDeleted()).isTrue();
        assertThat(trip.getDeletedAt()).isEqualTo(firstDeletedAt);
    }

    @Test
    void reconstituteRestoresPersistedState() {
        ScheduledTripId id = ScheduledTripId.of(UUID.randomUUID());
        RouteTemplateId routeTemplateId = RouteTemplateId.of(UUID.randomUUID());
        TrainId trainId = TrainId.of(UUID.randomUUID());
        Instant departureTime = Instant.parse("2026-03-27T06:00:00Z");
        Instant arrivalTime = Instant.parse("2026-03-27T08:00:00Z");
        Instant createdAt = Instant.parse("2026-03-20T10:00:00Z");
        Instant deletedAt = Instant.parse("2026-03-21T10:00:00Z");

        ScheduledTrip trip = ScheduledTrip.reconstitute(
                id,
                routeTemplateId,
                trainId,
                departureTime,
                arrivalTime,
                ScheduledTripStatus.DEPARTED,
                createdAt,
                deletedAt);

        assertThat(trip.getId()).isEqualTo(id);
        assertThat(trip.getRouteTemplateId()).isEqualTo(routeTemplateId);
        assertThat(trip.getTrainId()).isEqualTo(trainId);
        assertThat(trip.getDepartureTime()).isEqualTo(departureTime);
        assertThat(trip.getArrivalTime()).isEqualTo(arrivalTime);
        assertThat(trip.getStatus()).isEqualTo(ScheduledTripStatus.DEPARTED);
        assertThat(trip.getCreatedAt()).isEqualTo(createdAt);
        assertThat(trip.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(trip.isDeleted()).isTrue();
    }

    private static ScheduledTrip createTrip() {
        return ScheduledTrip.create(
                ScheduledTripId.of(UUID.randomUUID()),
                RouteTemplateId.of(UUID.randomUUID()),
                TrainId.of(UUID.randomUUID()),
                Instant.parse("2026-03-27T06:00:00Z"),
                Instant.parse("2026-03-27T08:00:00Z"));
    }
}
