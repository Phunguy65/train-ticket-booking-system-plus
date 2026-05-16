package io.github.phunguy65.ttbs.backend.train.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.phunguy65.ttbs.backend.TestContainerConfiguration;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.shared.domain.SliceResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.train.application.query.GetScheduledTripByIdQuery;
import io.github.phunguy65.ttbs.backend.train.application.query.ScheduledTripSearchSortField;
import io.github.phunguy65.ttbs.backend.train.application.query.SearchScheduledTripsQuery;
import io.github.phunguy65.ttbs.backend.train.application.response.ScheduledTripDetailResponse;
import io.github.phunguy65.ttbs.backend.train.application.response.SearchScheduledTripsResponse;
import io.github.phunguy65.ttbs.backend.train.domain.error.ScheduledTripError;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainerConfiguration.class)
@DisplayName("SearchScheduledTripsUseCase stress")
class SearchScheduledTripsStressTest {

    @Autowired
    private SearchScheduledTripsUseCase searchScheduledTripsUseCase;

    @Autowired
    private GetScheduledTripByIdUseCase getScheduledTripByIdUseCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        cleanupTestData();
    }

    @AfterEach
    void tearDown() {
        cleanupTestData();
    }

    @Test
    @DisplayName("handles 50 concurrent requests with same query")
    void filter_handles50ConcurrentRequestsWithSameQuery() throws Exception {
        UUID originStationId = insertStation("TESTHN", "Ha Noi", "Ha Noi");
        UUID destinationStationId = insertStation("TESTDN", "Da Nang", "Da Nang");
        UUID trainId = insertTrain("TEST-SE1", "Test Express", 120);
        UUID routeTemplateId =
                insertRouteTemplate(originStationId, destinationStationId, 450000, "VND");
        Instant departureTime = Instant.parse("2026-06-01T08:00:00Z");
        insertScheduledTrip(
                routeTemplateId,
                trainId,
                departureTime,
                Instant.parse("2026-06-01T12:00:00Z"),
                "SCHEDULED");

        SearchScheduledTripsQuery query = new SearchScheduledTripsQuery(
                originStationId,
                null,
                LocalDate.parse("2026-06-01"),
                null,
                false,
                null,
                null,
                ScheduledTripSearchSortField.DEPARTURE_TIME,
                SortOrder.Direction.ASC,
                null,
                20);

        List<SliceResponse<SearchScheduledTripsResponse>> results =
                runConcurrentSearches(50, query);

        assertThat(results).hasSize(50);
        assertThat(results).allSatisfy(result -> {
            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).route().origin().id()).isEqualTo(originStationId);
            assertThat(result.content().get(0).departureTime()).isEqualTo(departureTime);
        });
        assertThat(results.stream().map(SliceResponse::content).distinct()).hasSize(1);
    }

    @Test
    @DisplayName("handles 50 concurrent requests for same scheduled trip")
    void getById_handles50ConcurrentRequestsForSameScheduledTrip() throws Exception {
        UUID originStationId = insertStation("TESTHP", "Hai Phong", "Hai Phong");
        UUID destinationStationId = insertStation("TESTSG", "Sai Gon", "Ho Chi Minh");
        UUID trainId = insertTrain("TEST-SE2", "Southbound Express", 150);
        UUID routeTemplateId =
                insertRouteTemplate(originStationId, destinationStationId, 950000, "VND");
        Instant departureTime = Instant.parse("2026-07-01T06:00:00Z");
        Instant arrivalTime = Instant.parse("2026-07-01T20:00:00Z");
        UUID scheduledTripId = insertScheduledTrip(
                routeTemplateId, trainId, departureTime, arrivalTime, "SCHEDULED");

        List<Result<ScheduledTripDetailResponse, ScheduledTripError>> results =
                runConcurrentGetById(50, scheduledTripId);

        assertThat(results).hasSize(50);
        assertThat(results).allMatch(Result::isSuccess);
        assertThat(results.stream()
                        .map(result -> ((Result.Success<
                                                ScheduledTripDetailResponse, ScheduledTripError>)
                                        result)
                                .value()
                                .id())
                        .distinct())
                .containsExactly(scheduledTripId);
        assertThat(results.stream()
                        .map(result -> ((Result.Success<
                                                ScheduledTripDetailResponse, ScheduledTripError>)
                                        result)
                                .value()
                                .departureTime())
                        .distinct())
                .containsExactly(departureTime);
    }

    private List<SliceResponse<SearchScheduledTripsResponse>> runConcurrentSearches(
            int threadCount, SearchScheduledTripsQuery query)
            throws InterruptedException, ExecutionException {
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            List<Future<SliceResponse<SearchScheduledTripsResponse>>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(searchTask(query, ready, start)));
            }

            ready.await(10, TimeUnit.SECONDS);
            start.countDown();

            List<SliceResponse<SearchScheduledTripsResponse>> results = new ArrayList<>();
            for (Future<SliceResponse<SearchScheduledTripsResponse>> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private List<Result<ScheduledTripDetailResponse, ScheduledTripError>> runConcurrentGetById(
            int threadCount, UUID scheduledTripId) throws InterruptedException, ExecutionException {
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            List<Future<Result<ScheduledTripDetailResponse, ScheduledTripError>>> futures =
                    new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(getByIdTask(scheduledTripId, ready, start)));
            }

            ready.await(10, TimeUnit.SECONDS);
            start.countDown();

            List<Result<ScheduledTripDetailResponse, ScheduledTripError>> results =
                    new ArrayList<>();
            for (Future<Result<ScheduledTripDetailResponse, ScheduledTripError>> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private Callable<SliceResponse<SearchScheduledTripsResponse>> searchTask(
            SearchScheduledTripsQuery query, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            start.await(10, TimeUnit.SECONDS);
            return searchScheduledTripsUseCase.execute(query);
        };
    }

    private Callable<Result<ScheduledTripDetailResponse, ScheduledTripError>> getByIdTask(
            UUID scheduledTripId, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            start.await(10, TimeUnit.SECONDS);
            return getScheduledTripByIdUseCase.execute(
                    new GetScheduledTripByIdQuery(scheduledTripId));
        };
    }

    private UUID insertStation(String code, String name, String city) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO stations (id, code, name, city, created_at) VALUES (?, ?, ?, ?, ?)",
                id,
                code,
                name,
                city,
                Timestamp.from(Instant.now()));
        return id;
    }

    private UUID insertTrain(String trainNumber, String name, int totalSeats) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO trains (id, train_number, name, total_seats, created_at) VALUES (?, ?, ?, ?, ?)",
                id,
                trainNumber,
                name,
                totalSeats,
                Timestamp.from(Instant.now()));
        return id;
    }

    private UUID insertRouteTemplate(
            UUID originStationId, UUID destinationStationId, long basePrice, String currency) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO route_templates (id, origin_station_id, destination_station_id, base_price, created_at) VALUES (?, ?, ?, ?, ?)",
                id,
                originStationId,
                destinationStationId,
                basePrice,
                Timestamp.from(Instant.now()));
        return id;
    }

    private UUID insertScheduledTrip(
            UUID routeTemplateId,
            UUID trainId,
            Instant departureTime,
            Instant arrivalTime,
            String status) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO scheduled_trips (id, route_template_id, train_id, departure_time, arrival_time, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                id,
                routeTemplateId,
                trainId,
                Timestamp.from(departureTime),
                Timestamp.from(arrivalTime),
                status,
                Timestamp.from(Instant.now()));
        return id;
    }

    private void cleanupTestData() {
        jdbcTemplate.update(
                "DELETE FROM scheduled_trips WHERE id IN (SELECT st.id FROM scheduled_trips st JOIN route_templates rt ON st.route_template_id = rt.id JOIN stations s ON rt.origin_station_id = s.id WHERE s.code LIKE 'TEST%')");
        jdbcTemplate.update(
                "DELETE FROM route_templates WHERE origin_station_id IN (SELECT id FROM stations WHERE code LIKE 'TEST%')");
        jdbcTemplate.update("DELETE FROM trains WHERE train_number LIKE 'TEST%'");
        jdbcTemplate.update("DELETE FROM stations WHERE code LIKE 'TEST%'");
    }
}
