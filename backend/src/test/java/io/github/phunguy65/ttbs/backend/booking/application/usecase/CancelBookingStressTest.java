package io.github.phunguy65.ttbs.backend.booking.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.phunguy65.ttbs.backend.TestContainerConfiguration;
import io.github.phunguy65.ttbs.backend.booking.application.command.CancelBookingCommand;
import io.github.phunguy65.ttbs.backend.booking.domain.error.BookingError;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
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
@DisplayName("CancelBooking stress")
class CancelBookingStressTest {

    private static final int THREAD_COUNT = 50;

    @Autowired
    private CancelBookingUseCase useCase;

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
    @DisplayName(
            "only one cancellation succeeds when 50 concurrent requests cancel the same booking")
    void execute_allowsOnlyOneCancellationForSameBookingUnderConcurrentLoad() throws Exception {
        TestCancelBookingData data =
                insertCancelBookingData("TESTCB1", "test-cancel-load@example.com", "1A");
        CancelBookingCommand command = new CancelBookingCommand(data.bookingId(), data.userId());

        List<Result<Void, BookingError>> results = runConcurrentCancels(THREAD_COUNT, command);

        assertThat(results).hasSize(THREAD_COUNT);
        assertThat(results.stream().filter(Result::isSuccess).count()).isEqualTo(1);
        assertThat(results.stream().filter(Result::isFailure).count()).isEqualTo(49);
        assertThat(bookingStatus(data.bookingId())).isEqualTo("CANCELLED");
        assertThat(seatStatus(data.scheduledTripId(), data.seatId())).isEqualTo("AVAILABLE");
    }

    private List<Result<Void, BookingError>> runConcurrentCancels(
            int threadCount, CancelBookingCommand command) throws Exception {
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            List<Future<Result<Void, BookingError>>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await(10, TimeUnit.SECONDS);
                    try {
                        return useCase.execute(command);
                    } catch (Exception exception) {
                        return Result.failure(
                                new BookingError.InvalidStatusTransition("CANCELLED", "CANCELLED"));
                    }
                }));
            }
            ready.await(10, TimeUnit.SECONDS);
            start.countDown();
            List<Result<Void, BookingError>> results = new ArrayList<>();
            for (var future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private TestCancelBookingData insertCancelBookingData(
            String stationCodePrefix, String email, String seatNumber) {
        UUID userId = insertUser(email);
        UUID originStationId = insertStation(stationCodePrefix + "O", "Origin", "Ha Noi");
        UUID destinationStationId =
                insertStation(stationCodePrefix + "D", "Destination", "Da Nang");
        UUID trainId = insertTrain(stationCodePrefix + "T", "Cancel Express", 1);
        UUID coachId = insertCoach(trainId, 1, 1);
        UUID seatId = insertSeat(coachId, seatNumber);
        UUID routeTemplateId = insertRouteTemplate(originStationId, destinationStationId, 500000);
        UUID scheduledTripId = insertScheduledTrip(routeTemplateId, trainId);
        UUID bookingId = insertBooking(userId, scheduledTripId, seatId, stationCodePrefix);
        insertHeldTripSeatAvailability(scheduledTripId, seatId, bookingId);
        return new TestCancelBookingData(userId, scheduledTripId, seatId, bookingId);
    }

    private UUID insertUser(String email) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, full_name, role, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                id,
                email,
                "hash",
                "Test Cancel User",
                "CUSTOMER",
                Timestamp.from(Instant.now()),
                Timestamp.from(Instant.now()));
        return id;
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

    private UUID insertScheduledTrip(UUID routeTemplateId, UUID trainId) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO scheduled_trips (id, route_template_id, train_id, departure_time, arrival_time, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                id,
                routeTemplateId,
                trainId,
                Timestamp.from(Instant.parse("2026-08-01T08:00:00Z")),
                Timestamp.from(Instant.parse("2026-08-01T12:00:00Z")),
                "SCHEDULED",
                Timestamp.from(Instant.now()));
        return id;
    }

    private UUID insertBooking(
            UUID userId, UUID scheduledTripId, UUID seatId, String idempotencyKey) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO bookings (id, user_id, scheduled_trip_id, user_info_snapshot, passengers_snapshot, total_price, currency, status, idempotency_key, payment_deadline, created_at) VALUES (?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), ?, ?, 'HELD', ?, ?, ?)",
                id,
                userId,
                scheduledTripId,
                userInfoSnapshot(),
                passengerSnapshot(seatId),
                500000,
                "VND",
                "cancel-" + idempotencyKey,
                Timestamp.from(Instant.parse("2026-08-01T07:45:00Z")),
                Timestamp.from(Instant.now()));
        return id;
    }

    private void insertHeldTripSeatAvailability(UUID scheduledTripId, UUID seatId, UUID bookingId) {
        jdbcTemplate.update(
                "INSERT INTO trip_seat_availability (scheduled_trip_id, seat_id, status, booking_id, version) VALUES (?, ?, 'HELD', ?, 1)",
                scheduledTripId,
                seatId,
                bookingId);
    }

    private String bookingStatus(UUID bookingId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM bookings WHERE id = ?", String.class, bookingId);
    }

    private String seatStatus(UUID scheduledTripId, UUID seatId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM trip_seat_availability WHERE scheduled_trip_id = ? AND seat_id = ?",
                String.class,
                scheduledTripId,
                seatId);
    }

    private String userInfoSnapshot() {
        return "{"
                + "\"fullName\":\"Nguyen Van A\","
                + "\"email\":\"a@example.com\","
                + "\"phone\":\"0900000000\","
                + "\"dateOfBirth\":null,"
                + "\"gender\":\"MALE\","
                + "\"idDocumentNumber\":\"0123456789\","
                + "\"addressLine\":\"123 Test Street\"}";
    }

    private String passengerSnapshot(UUID seatId) {
        return "[{"
                + "\"seatId\":\"" + seatId + "\","
                + "\"fullName\":\"Nguyen Van A\","
                + "\"idDocumentNumber\":\"0123456789\","
                + "\"dateOfBirth\":\"1990-01-01\","
                + "\"gender\":\"MALE\"}]";
    }

    private void cleanupTestData() {
        jdbcTemplate.update(
                "DELETE FROM trip_seat_availability WHERE scheduled_trip_id IN (SELECT st.id FROM scheduled_trips st JOIN route_templates rt ON st.route_template_id = rt.id JOIN stations s ON rt.origin_station_id = s.id WHERE s.code LIKE 'TESTCB%')");
        jdbcTemplate.update(
                "DELETE FROM bookings WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'test-cancel-%')");
        jdbcTemplate.update(
                "DELETE FROM scheduled_trips WHERE id IN (SELECT st.id FROM scheduled_trips st JOIN route_templates rt ON st.route_template_id = rt.id JOIN stations s ON rt.origin_station_id = s.id WHERE s.code LIKE 'TESTCB%')");
        jdbcTemplate.update(
                "DELETE FROM route_templates WHERE origin_station_id IN (SELECT id FROM stations WHERE code LIKE 'TESTCB%')");
        jdbcTemplate.update(
                "DELETE FROM seats WHERE coach_id IN (SELECT id FROM coaches WHERE train_id IN (SELECT id FROM trains WHERE train_number LIKE 'TESTCB%'))");
        jdbcTemplate.update(
                "DELETE FROM coaches WHERE train_id IN (SELECT id FROM trains WHERE train_number LIKE 'TESTCB%')");
        jdbcTemplate.update("DELETE FROM trains WHERE train_number LIKE 'TESTCB%'");
        jdbcTemplate.update("DELETE FROM stations WHERE code LIKE 'TESTCB%'");
        jdbcTemplate.update("DELETE FROM users WHERE email LIKE 'test-cancel-%'");
    }

    private record TestCancelBookingData(
            UUID userId, UUID scheduledTripId, UUID seatId, UUID bookingId) {}
}
