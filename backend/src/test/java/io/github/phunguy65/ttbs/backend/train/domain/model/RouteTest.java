package io.github.phunguy65.ttbs.backend.train.domain.model;

import static org.assertj.core.api.Assertions.*;

import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import io.github.phunguy65.ttbs.backend.train.domain.event.RouteCreated;
import io.github.phunguy65.ttbs.backend.train.domain.event.RouteDeleted;
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

    private Route newRoute() {
        return Route.create(ROUTE_ID, TRAIN_ID, ORIGIN, DESTINATION, DEPARTURE, ARRIVAL, PRICE);
    }

    // ── create ───────────────────────────────────────────────────────────────

    @Test
    void create_shouldSetScheduledStatus() {
        Route route = newRoute();

        assertThat(route.getStatus()).isEqualTo(RouteStatus.SCHEDULED);
    }

    @Test
    void create_shouldRegisterExactlyOneRouteCreatedEvent() {
        Route route = newRoute();

        assertThat(route.getDomainEvents()).hasSize(1);
        assertThat(route.getDomainEvents().getFirst()).isInstanceOf(RouteCreated.class);
    }

    @Test
    void create_routeCreatedEvent_shouldContainCorrectIds() {
        Route route = newRoute();

        RouteCreated event = (RouteCreated) route.getDomainEvents().getFirst();
        assertThat(event.routeId()).isEqualTo(ROUTE_ID);
        assertThat(event.trainId()).isEqualTo(TRAIN_ID);
        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    void create_shouldSetCorrectFields() {
        Route route = newRoute();

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
    void create_shouldHaveNullDeletedAt() {
        Route route = newRoute();

        assertThat(route.getDeletedAt()).isNull();
        assertThat(route.isDeleted()).isFalse();
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

    // ── softDelete ───────────────────────────────────────────────────────────

    @Test
    void softDelete_shouldSetDeletedAt() {
        Route route = newRoute();
        route.clearDomainEvents();

        route.softDelete();

        assertThat(route.getDeletedAt()).isNotNull();
        assertThat(route.isDeleted()).isTrue();
    }

    @Test
    void softDelete_shouldRegisterRouteDeletedEvent() {
        Route route = newRoute();
        route.clearDomainEvents();

        route.softDelete();

        assertThat(route.getDomainEvents()).hasSize(1);
        assertThat(route.getDomainEvents().getFirst()).isInstanceOf(RouteDeleted.class);
        RouteDeleted event = (RouteDeleted) route.getDomainEvents().getFirst();
        assertThat(event.routeId()).isEqualTo(ROUTE_ID);
        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    void softDelete_idempotent_secondCallIsNoOp() {
        Route route = newRoute();
        route.clearDomainEvents();

        route.softDelete();
        Instant firstDeletedAt = route.getDeletedAt();
        route.clearDomainEvents();

        route.softDelete(); // second call — should be a no-op

        assertThat(route.getDeletedAt()).isEqualTo(firstDeletedAt);
        assertThat(route.getDomainEvents()).isEmpty();
    }

    // ── reconstitute ─────────────────────────────────────────────────────────

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
                createdAt,
                null);

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
                createdAt,
                null);

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

    @Test
    void reconstitute_withDeletedAt_shouldReportIsDeleted() {
        Instant deletedAt = Instant.parse("2025-01-01T00:00:00Z");

        Route route = Route.reconstitute(
                ROUTE_ID,
                TRAIN_ID,
                ORIGIN,
                DESTINATION,
                DEPARTURE,
                ARRIVAL,
                PRICE,
                RouteStatus.SCHEDULED,
                DEPARTURE,
                deletedAt);

        assertThat(route.isDeleted()).isTrue();
        assertThat(route.getDeletedAt()).isEqualTo(deletedAt);
    }

    @Test
    void reconstitute_withNullDeletedAt_shouldReportNotDeleted() {
        Route route = Route.reconstitute(
                ROUTE_ID,
                TRAIN_ID,
                ORIGIN,
                DESTINATION,
                DEPARTURE,
                ARRIVAL,
                PRICE,
                RouteStatus.SCHEDULED,
                DEPARTURE,
                null);

        assertThat(route.isDeleted()).isFalse();
    }
}
