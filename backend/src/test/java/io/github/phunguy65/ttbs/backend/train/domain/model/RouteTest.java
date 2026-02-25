package io.github.phunguy65.ttbs.backend.train.domain.model;

import static org.assertj.core.api.Assertions.*;

import io.github.phunguy65.ttbs.backend.train.domain.event.RouteCreated;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RouteTest {

    private static final RouteId ROUTE_ID = RouteId.of(UUID.randomUUID());
    private static final TrainId TRAIN_ID = TrainId.of(UUID.randomUUID());
    private static final StationId ORIGIN = StationId.of(UUID.randomUUID());
    private static final StationId DESTINATION = StationId.of(UUID.randomUUID());
    private static final Instant DEPARTURE = Instant.parse("2025-06-01T08:00:00Z");
    private static final Instant ARRIVAL = Instant.parse("2025-06-01T12:00:00Z");
    private static final BigDecimal PRICE = new BigDecimal("150.00");

    @Test
    void create_shouldSetScheduledStatus() {
        Route route =
                Route.create(ROUTE_ID, TRAIN_ID, ORIGIN, DESTINATION, DEPARTURE, ARRIVAL, PRICE);

        assertThat(route.getStatus()).isEqualTo(RouteStatus.SCHEDULED);
    }

    @Test
    void create_shouldRegisterExactlyOneRouteCreatedEvent() {
        Route route =
                Route.create(ROUTE_ID, TRAIN_ID, ORIGIN, DESTINATION, DEPARTURE, ARRIVAL, PRICE);

        assertThat(route.getDomainEvents()).hasSize(1);
        assertThat(route.getDomainEvents().getFirst()).isInstanceOf(RouteCreated.class);
    }

    @Test
    void create_routeCreatedEvent_shouldContainCorrectIds() {
        Route route =
                Route.create(ROUTE_ID, TRAIN_ID, ORIGIN, DESTINATION, DEPARTURE, ARRIVAL, PRICE);

        RouteCreated event = (RouteCreated) route.getDomainEvents().getFirst();
        assertThat(event.routeId()).isEqualTo(ROUTE_ID);
        assertThat(event.trainId()).isEqualTo(TRAIN_ID);
        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    void create_shouldSetCorrectFields() {
        Route route =
                Route.create(ROUTE_ID, TRAIN_ID, ORIGIN, DESTINATION, DEPARTURE, ARRIVAL, PRICE);

        assertThat(route.getId()).isEqualTo(ROUTE_ID);
        assertThat(route.getTrainId()).isEqualTo(TRAIN_ID);
        assertThat(route.getOriginStationId()).isEqualTo(ORIGIN);
        assertThat(route.getDestinationStationId()).isEqualTo(DESTINATION);
        assertThat(route.getDepartureTime()).isEqualTo(DEPARTURE);
        assertThat(route.getArrivalTime()).isEqualTo(ARRIVAL);
        assertThat(route.getBasePrice()).isEqualByComparingTo(PRICE);
        assertThat(route.getCreatedAt()).isNotNull();
    }

    @Test
    void create_whenArrivalNotAfterDeparture_shouldThrowIllegalArgumentException() {
        Instant sameTime = DEPARTURE;

        assertThatThrownBy(() -> Route.create(
                        ROUTE_ID, TRAIN_ID, ORIGIN, DESTINATION, DEPARTURE, sameTime, PRICE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("arrivalTime must be after departureTime");
    }

    @Test
    void create_whenArrivalBeforeDeparture_shouldThrowIllegalArgumentException() {
        Instant beforeDeparture = DEPARTURE.minusSeconds(3600);

        assertThatThrownBy(() -> Route.create(
                        ROUTE_ID, TRAIN_ID, ORIGIN, DESTINATION, DEPARTURE, beforeDeparture, PRICE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("arrivalTime must be after departureTime");
    }

    @Test
    void reconstitute_shouldNotRegisterDomainEvents() {
        Instant createdAt = Instant.parse("2024-01-15T10:00:00Z");

        Route route = Route.reconstitute(
                ROUTE_ID,
                TRAIN_ID,
                ORIGIN,
                DESTINATION,
                DEPARTURE,
                ARRIVAL,
                PRICE,
                RouteStatus.SCHEDULED,
                createdAt);

        assertThat(route.getDomainEvents()).isEmpty();
    }

    @Test
    void reconstitute_shouldRestoreAllFields() {
        Instant createdAt = Instant.parse("2024-01-15T10:00:00Z");

        Route route = Route.reconstitute(
                ROUTE_ID,
                TRAIN_ID,
                ORIGIN,
                DESTINATION,
                DEPARTURE,
                ARRIVAL,
                PRICE,
                RouteStatus.SCHEDULED,
                createdAt);

        assertThat(route.getId()).isEqualTo(ROUTE_ID);
        assertThat(route.getTrainId()).isEqualTo(TRAIN_ID);
        assertThat(route.getOriginStationId()).isEqualTo(ORIGIN);
        assertThat(route.getDestinationStationId()).isEqualTo(DESTINATION);
        assertThat(route.getDepartureTime()).isEqualTo(DEPARTURE);
        assertThat(route.getArrivalTime()).isEqualTo(ARRIVAL);
        assertThat(route.getBasePrice()).isEqualByComparingTo(PRICE);
        assertThat(route.getStatus()).isEqualTo(RouteStatus.SCHEDULED);
        assertThat(route.getCreatedAt()).isEqualTo(createdAt);
    }
}
