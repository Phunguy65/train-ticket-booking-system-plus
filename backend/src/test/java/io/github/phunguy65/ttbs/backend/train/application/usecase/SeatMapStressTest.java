package io.github.phunguy65.ttbs.backend.train.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.phunguy65.ttbs.backend.TestContainerConfiguration;
import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.query.GetAvailableSeatsQuery;
import io.github.phunguy65.ttbs.backend.train.application.query.GetCoachSeatMapQuery;
import io.github.phunguy65.ttbs.backend.train.application.response.CoachSeatMapResponse;
import io.github.phunguy65.ttbs.backend.train.application.response.SeatResponse;
import io.github.phunguy65.ttbs.backend.train.domain.error.ScheduledTripError;
import java.sql.Timestamp;
import java.time.Instant;
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
@DisplayName("SeatMap stress")
class SeatMapStressTest {

    @Autowired
    private GetAvailableSeatsForScheduledTripUseCase getAvailableSeatsForScheduledTripUseCase;

    @Autowired
    private GetCoachSeatMapByScheduledTripUseCase getCoachSeatMapByScheduledTripUseCase;

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
    @DisplayName("handles 50 concurrent available seats requests")
    void getAvailableSeats_handles50ConcurrentRequests() throws Exception {
        UUID stationOriginId = insertStation("TESTHN", "Ha Noi", "Ha Noi");
        UUID stationDestId = insertStation("TESTDN", "Da Nang", "Da Nang");
        UUID trainId = insertTrain("TEST-SE1", "Test Express", 120);
        UUID coachId = insertCoach(trainId, 1, 4);
        UUID seat1Id = insertSeat(coachId, "1A");
        UUID seat2Id = insertSeat(coachId, "1B");
        UUID seat3Id = insertSeat(coachId, "2A");
        UUID seat4Id = insertSeat(coachId, "2B");
        UUID routeTemplateId = insertRouteTemplate(stationOriginId, stationDestId, 450000);
        UUID scheduledTripId = insertScheduledTrip(
                routeTemplateId,
                trainId,
                Instant.parse("2026-06-01T08:00:00Z"),
                Instant.parse("2026-06-01T12:00:00Z"),
                "SCHEDULED");
        insertTripSeatAvailability(scheduledTripId, seat1Id, "AVAILABLE");
        insertTripSeatAvailability(scheduledTripId, seat2Id, "AVAILABLE");
        insertTripSeatAvailability(scheduledTripId, seat3Id, "BOOKED");
        insertTripSeatAvailability(scheduledTripId, seat4Id, "AVAILABLE");

        GetAvailableSeatsQuery query = new GetAvailableSeatsQuery(0, 20, scheduledTripId);
        List<PageResponse<SeatResponse>> results = runConcurrentAvailableSeats(50, query);

        assertThat(results).hasSize(50);
        assertThat(results).allSatisfy(result -> {
            assertThat(result.content()).hasSize(3);
            assertThat(result.content())
                    .extracting(SeatResponse::seatNumber)
                    .containsExactly("1A", "1B", "2B");
        });
        assertThat(results.stream().map(PageResponse::content).distinct()).hasSize(1);
    }

    @Test
    @DisplayName("handles 50 concurrent coach seat map requests with cache")
    void getCoachSeatMap_handles50ConcurrentRequestsWithCache() throws Exception {
        UUID stationOriginId = insertStation("TESTHP", "Hai Phong", "Hai Phong");
        UUID stationDestId = insertStation("TESTSG", "Sai Gon", "Ho Chi Minh");
        UUID trainId = insertTrain("TEST-SE2", "Southbound Express", 150);
        UUID coach1Id = insertCoach(trainId, 1, 2);
        UUID coach2Id = insertCoach(trainId, 2, 2);
        UUID seat1Id = insertSeat(coach1Id, "1A");
        UUID seat2Id = insertSeat(coach1Id, "1B");
        UUID seat3Id = insertSeat(coach2Id, "2A");
        UUID seat4Id = insertSeat(coach2Id, "2B");
        UUID routeTemplateId = insertRouteTemplate(stationOriginId, stationDestId, 950000);
        UUID scheduledTripId = insertScheduledTrip(
                routeTemplateId,
                trainId,
                Instant.parse("2026-07-01T06:00:00Z"),
                Instant.parse("2026-07-01T20:00:00Z"),
                "SCHEDULED");
        insertTripSeatAvailability(scheduledTripId, seat1Id, "AVAILABLE");
        insertTripSeatAvailability(scheduledTripId, seat2Id, "HELD");
        insertTripSeatAvailability(scheduledTripId, seat3Id, "BOOKED");
        insertTripSeatAvailability(scheduledTripId, seat4Id, "AVAILABLE");

        GetCoachSeatMapQuery query = new GetCoachSeatMapQuery(0, 20, scheduledTripId);
        List<Result<PageResponse<CoachSeatMapResponse>, ScheduledTripError>> results =
                runConcurrentCoachSeatMap(50, query);

        assertThat(results).hasSize(50);
        assertThat(results).allMatch(Result::isSuccess);
        assertThat(results.stream()
                        .map(r -> ((Result.Success<
                                                PageResponse<CoachSeatMapResponse>,
                                                ScheduledTripError>)
                                        r)
                                .value()
                                .content()
                                .size())
                        .distinct())
                .containsExactly(2);
    }

    private List<PageResponse<SeatResponse>> runConcurrentAvailableSeats(
            int threadCount, GetAvailableSeatsQuery query)
            throws InterruptedException, ExecutionException {
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            List<Future<PageResponse<SeatResponse>>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(availableSeatsTask(query, ready, start)));
            }

            ready.await(10, TimeUnit.SECONDS);
            start.countDown();

            List<PageResponse<SeatResponse>> results = new ArrayList<>();
            for (Future<PageResponse<SeatResponse>> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private List<Result<PageResponse<CoachSeatMapResponse>, ScheduledTripError>>
            runConcurrentCoachSeatMap(int threadCount, GetCoachSeatMapQuery query)
                    throws InterruptedException, ExecutionException {
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            List<Future<Result<PageResponse<CoachSeatMapResponse>, ScheduledTripError>>> futures =
                    new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(coachSeatMapTask(query, ready, start)));
            }

            ready.await(10, TimeUnit.SECONDS);
            start.countDown();

            List<Result<PageResponse<CoachSeatMapResponse>, ScheduledTripError>> results =
                    new ArrayList<>();
            for (Future<Result<PageResponse<CoachSeatMapResponse>, ScheduledTripError>> future :
                    futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private Callable<PageResponse<SeatResponse>> availableSeatsTask(
            GetAvailableSeatsQuery query, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            start.await(10, TimeUnit.SECONDS);
            return getAvailableSeatsForScheduledTripUseCase.execute(query);
        };
    }

    private Callable<Result<PageResponse<CoachSeatMapResponse>, ScheduledTripError>>
            coachSeatMapTask(
                    GetCoachSeatMapQuery query, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            start.await(10, TimeUnit.SECONDS);
            return getCoachSeatMapByScheduledTripUseCase.execute(query);
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

    private UUID insertCoach(UUID trainId, int carNumber, int totalSeats) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO coaches (id, train_id, car_number, total_seats, created_at) VALUES (?, ?, ?, ?, ?)",
                id,
                trainId,
                carNumber,
                totalSeats,
                Timestamp.from(Instant.now()));
        return id;
    }

    private UUID insertSeat(UUID coachId, String seatNumber) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO seats (id, coach_id, seat_number, created_at) VALUES (?, ?, ?, ?)",
                id,
                coachId,
                seatNumber,
                Timestamp.from(Instant.now()));
        return id;
    }

    private UUID insertRouteTemplate(
            UUID originStationId, UUID destinationStationId, long basePrice) {
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

    private void insertTripSeatAvailability(UUID scheduledTripId, UUID seatId, String status) {
        jdbcTemplate.update(
                "INSERT INTO trip_seat_availability (scheduled_trip_id, seat_id, status, version) VALUES (?, ?, ?, 1)",
                scheduledTripId,
                seatId,
                status);
    }

    private void cleanupTestData() {
        jdbcTemplate.update(
                "DELETE FROM trip_seat_availability WHERE scheduled_trip_id IN (SELECT st.id FROM scheduled_trips st JOIN route_templates rt ON st.route_template_id = rt.id JOIN stations s ON rt.origin_station_id = s.id WHERE s.code LIKE 'TEST%')");
        jdbcTemplate.update(
                "DELETE FROM scheduled_trips WHERE id IN (SELECT st.id FROM scheduled_trips st JOIN route_templates rt ON st.route_template_id = rt.id JOIN stations s ON rt.origin_station_id = s.id WHERE s.code LIKE 'TEST%')");
        jdbcTemplate.update(
                "DELETE FROM route_templates WHERE origin_station_id IN (SELECT id FROM stations WHERE code LIKE 'TEST%')");
        jdbcTemplate.update(
                "DELETE FROM seats WHERE coach_id IN (SELECT id FROM coaches WHERE train_id IN (SELECT id FROM trains WHERE train_number LIKE 'TEST%'))");
        jdbcTemplate.update(
                "DELETE FROM coaches WHERE train_id IN (SELECT id FROM trains WHERE train_number LIKE 'TEST%')");
        jdbcTemplate.update("DELETE FROM trains WHERE train_number LIKE 'TEST%'");
        jdbcTemplate.update("DELETE FROM stations WHERE code LIKE 'TEST%'");
    }
}
