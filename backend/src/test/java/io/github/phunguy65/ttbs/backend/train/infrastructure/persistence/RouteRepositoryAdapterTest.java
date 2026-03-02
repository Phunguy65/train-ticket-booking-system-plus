package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResult;
import io.github.phunguy65.ttbs.backend.shared.domain.SortDirection;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import io.github.phunguy65.ttbs.backend.train.domain.model.Route;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteFilter;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@Import({RouteRepositoryAdapter.class, RouteEntityMapper.class})
@TestPropertySource(properties = "spring.modulith.detection.disabled=true")
class RouteRepositoryAdapterTest {

    @Autowired
    private RouteRepository routeRepository;

    private static final TrainId TRAIN_ID = TrainId.of(UUID.randomUUID());
    private static final StationId ORIGIN_A = StationId.of(UUID.randomUUID());
    private static final StationId DEST_B = StationId.of(UUID.randomUUID());
    private static final Instant DEPARTURE_BASE = Instant.parse("2025-06-01T08:00:00Z");
    private static final Instant ARRIVAL_BASE = Instant.parse("2025-06-01T12:00:00Z");

    private Route newRoute() {
        return Route.create(
                RouteId.of(UUID.randomUUID()),
                TRAIN_ID,
                ORIGIN_A,
                DEST_B,
                DEPARTURE_BASE,
                ARRIVAL_BASE,
                new BigDecimal("150.00"));
    }

    private Route newRoute(StationId origin, StationId dest, Instant departure, Instant arrival) {
        return Route.create(
                RouteId.of(UUID.randomUUID()),
                TRAIN_ID,
                origin,
                dest,
                departure,
                arrival,
                new BigDecimal("100.00"));
    }

    // ── save ────────────────────────────────────────────────────────────────────

    @Test
    void save_shouldPersistRouteAndReturnDomainModel() {
        Route route = newRoute();

        Route saved = routeRepository.save(route);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTrainId()).isEqualTo(TRAIN_ID);
        assertThat(saved.getOriginStationId()).isEqualTo(ORIGIN_A);
        assertThat(saved.getDestinationStationId()).isEqualTo(DEST_B);
        assertThat(saved.getBasePrice()).isEqualByComparingTo("150.00");
        assertThat(saved.getDomainEvents()).isEmpty(); // reconstituted, no events
    }

    // ── findById ────────────────────────────────────────────────────────────────

    @Test
    void findById_existingId_shouldReturnRoute() {
        Route saved = routeRepository.save(newRoute());

        Optional<Route> found = routeRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getDomainEvents()).isEmpty();
    }

    @Test
    void findById_missingId_shouldReturnEmpty() {
        Optional<Route> found = routeRepository.findById(RouteId.of(UUID.randomUUID()));

        assertThat(found).isEmpty();
    }

    // ── findAll — no filter ──────────────────────────────────────────────────────

    @Test
    void findAll_emptyDatabase_returnsEmptyPageResult() {
        PageResult<Route> result = routeRepository.findAll(
                0, 20, "createdAt", SortDirection.DESC, RouteFilter.empty());

        assertThat(result.items()).isEmpty();
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isFalse();
    }

    @Test
    void findAll_firstPage_returnsItemsWithCorrectMetadata() {
        for (int i = 0; i < 5; i++) {
            routeRepository.save(newRoute());
        }

        PageResult<Route> result =
                routeRepository.findAll(0, 3, "createdAt", SortDirection.DESC, RouteFilter.empty());

        assertThat(result.items()).hasSize(3);
        assertThat(result.pageNumber()).isEqualTo(0);
        assertThat(result.pageSize()).isEqualTo(3);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.hasPrevious()).isFalse();
    }

    @Test
    void findAll_lastPage_hasNextFalseHasPreviousTrue() {
        for (int i = 0; i < 4; i++) {
            routeRepository.save(newRoute());
        }

        PageResult<Route> result =
                routeRepository.findAll(1, 3, "createdAt", SortDirection.ASC, RouteFilter.empty());

        assertThat(result.items()).hasSize(1);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isTrue();
    }

    // ── findAll — originStationId filter ────────────────────────────────────────

    @Test
    void findAll_withOriginFilter_returnsOnlyMatchingRoutes() {
        StationId otherOrigin = StationId.of(UUID.randomUUID());
        routeRepository.save(newRoute(ORIGIN_A, DEST_B, DEPARTURE_BASE, ARRIVAL_BASE));
        routeRepository.save(newRoute(
                ORIGIN_A,
                DEST_B,
                DEPARTURE_BASE.plusSeconds(3600),
                ARRIVAL_BASE.plusSeconds(3600)));
        routeRepository.save(newRoute(otherOrigin, DEST_B, DEPARTURE_BASE, ARRIVAL_BASE));

        RouteFilter filter = new RouteFilter(ORIGIN_A.value(), null, null, null);
        PageResult<Route> result =
                routeRepository.findAll(0, 20, "createdAt", SortDirection.DESC, filter);

        assertThat(result.items()).hasSize(2);
        assertThat(result.items()).allMatch(r -> r.getOriginStationId().equals(ORIGIN_A));
    }

    // ── findAll — departure date range filter ────────────────────────────────────

    @Test
    void findAll_withDepartureDateRangeFilter_returnsOnlyMatchingRoutes() {
        Instant early = Instant.parse("2025-05-01T08:00:00Z");
        Instant earlyArrival = early.plusSeconds(4 * 3600);
        Instant late = Instant.parse("2025-07-01T08:00:00Z");
        Instant lateArrival = late.plusSeconds(4 * 3600);

        routeRepository.save(newRoute(ORIGIN_A, DEST_B, early, earlyArrival));
        routeRepository.save(newRoute(ORIGIN_A, DEST_B, DEPARTURE_BASE, ARRIVAL_BASE));
        routeRepository.save(newRoute(ORIGIN_A, DEST_B, late, lateArrival));

        Instant from = Instant.parse("2025-05-15T00:00:00Z");
        Instant to = Instant.parse("2025-06-15T00:00:00Z");
        RouteFilter filter = new RouteFilter(null, null, from, to);
        PageResult<Route> result =
                routeRepository.findAll(0, 20, "departureTime", SortDirection.ASC, filter);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().getDepartureTime()).isEqualTo(DEPARTURE_BASE);
    }

    // ── soft delete ──────────────────────────────────────────────────────────

    @Test
    void softDeleteById_shouldSetDeletedAtAndExcludeFromFindById() {
        Route saved = routeRepository.save(newRoute());
        Instant now = Instant.now();

        routeRepository.softDeleteById(saved.getId(), now);

        Optional<Route> found = routeRepository.findById(saved.getId());
        assertThat(found).isEmpty();
    }

    @Test
    void softDeleteByIds_shouldSetDeletedAtForAllIds() {
        Route r1 = routeRepository.save(newRoute());
        Route r2 = routeRepository.save(newRoute());
        Instant now = Instant.now();

        int affected = routeRepository.softDeleteByIds(List.of(r1.getId(), r2.getId()), now);

        assertThat(affected).isEqualTo(2);
        assertThat(routeRepository.findById(r1.getId())).isEmpty();
        assertThat(routeRepository.findById(r2.getId())).isEmpty();
    }

    @Test
    void existsById_activeRoute_shouldReturnTrue() {
        Route saved = routeRepository.save(newRoute());

        assertThat(routeRepository.existsById(saved.getId())).isTrue();
    }

    @Test
    void existsById_deletedRoute_shouldReturnFalse() {
        Route saved = routeRepository.save(newRoute());
        routeRepository.softDeleteById(saved.getId(), Instant.now());

        assertThat(routeRepository.existsById(saved.getId())).isFalse();
    }

    @Test
    void findAll_shouldExcludeDeletedRoutes() {
        routeRepository.save(newRoute()); // active
        Route toDelete = routeRepository.save(newRoute());
        routeRepository.softDeleteById(toDelete.getId(), Instant.now());

        PageResult<Route> result = routeRepository.findAll(
                0, 20, "createdAt", SortDirection.DESC, RouteFilter.empty());

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().getId()).isNotEqualTo(toDelete.getId());
    }
}
