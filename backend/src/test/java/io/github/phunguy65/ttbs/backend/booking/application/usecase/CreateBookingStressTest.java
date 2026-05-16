package io.github.phunguy65.ttbs.backend.booking.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.phunguy65.ttbs.backend.TestContainerConfiguration;
import io.github.phunguy65.ttbs.backend.booking.application.command.CreateBookingCommand;
import io.github.phunguy65.ttbs.backend.booking.application.response.BookingResponse;
import io.github.phunguy65.ttbs.backend.booking.domain.error.BookingError;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainerConfiguration.class)
@DisplayName("CreateBooking stress")
class CreateBookingStressTest {

    @Autowired
    private CreateBookingUseCase useCase;

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
    @DisplayName("only one booking succeeds when 50 concurrent requests try to book the same seat")
    void execute_allowsOnlyOneBookingForSameSeatUnderConcurrentLoad() throws Exception {
        TestBookingData data = insertBookingData("TESTBK1", "test-booking-load@example.com", "1A");

        List<Result<BookingResponse, BookingError>> results = runConcurrentBookings(
                50,
                index -> command(
                        data.userId(),
                        data.scheduledTripId(),
                        data.seatId(),
                        "idem-load-" + index,
                        "ID" + index));

        assertThat(results).hasSize(50);
        assertThat(results.stream().filter(Result::isSuccess).count()).isEqualTo(1);
        assertThat(results.stream().filter(Result::isFailure).count()).isEqualTo(49);
        assertThat(results.stream()
                        .filter(Result::isFailure)
                        .map(result -> ((Result.Failure<?, BookingError>) result).error()))
                .allSatisfy(error -> assertThat(error)
                        .isInstanceOfAny(
                                BookingError.SeatNotAvailable.class,
                                BookingError.ActiveHoldExists.class));
    }

    @Test
    @DisplayName("idempotency under concurrent load returns same booking")
    void execute_returnsSameBookingForSameIdempotencyKeyUnderConcurrentLoad() throws Exception {
        TestBookingData data = insertBookingData("TESTBK2", "test-booking-idem@example.com", "1B");
        Result<BookingResponse, BookingError> initialResult = useCase.execute(command(
                data.userId(), data.scheduledTripId(), data.seatId(), "idem-shared", "ID-SHARED"));

        assertThat(initialResult.isSuccess()).isTrue();

        List<Result<BookingResponse, BookingError>> results = runConcurrentBookings(
                50,
                index -> command(
                        data.userId(),
                        data.scheduledTripId(),
                        data.seatId(),
                        "idem-shared",
                        "ID-SHARED"));

        assertThat(results).hasSize(50);
        assertThat(results).allMatch(Result::isSuccess);
        assertThat(results.stream()
                        .map(result -> ((Result.Success<BookingResponse, BookingError>) result)
                                .value()
                                .id())
                        .distinct())
                .hasSize(1);
    }

    private List<Result<BookingResponse, BookingError>> runConcurrentBookings(
            int threadCount, CommandFactory commandFactory) throws Exception {
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            List<Future<Result<BookingResponse, BookingError>>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                int index = i;
                futures.add(
                        executor.submit(bookingTask(commandFactory.create(index), ready, start)));
            }
            ready.await(10, TimeUnit.SECONDS);
            start.countDown();
            List<Result<BookingResponse, BookingError>> results = new ArrayList<>();
            for (Future<Result<BookingResponse, BookingError>> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private Callable<Result<BookingResponse, BookingError>> bookingTask(
            CreateBookingCommand command, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            start.await(10, TimeUnit.SECONDS);
            try {
                return useCase.execute(command);
            } catch (DataIntegrityViolationException exception) {
                return Result.failure(new BookingError.SeatNotAvailable());
            }
        };
    }

    private CreateBookingCommand command(
            UUID userId,
            UUID scheduledTripId,
            UUID seatId,
            String idempotencyKey,
            String idDocumentNumber) {
        return new CreateBookingCommand(
                userId,
                scheduledTripId,
                List.of(SeatId.of(seatId)),
                List.of(new CreateBookingCommand.PassengerPayload(
                        SeatId.of(seatId),
                        "Name",
                        idDocumentNumber,
                        LocalDate.of(1990, 1, 1),
                        "Male")),
                idempotencyKey);
    }

    private TestBookingData insertBookingData(
            String stationCodePrefix, String email, String seatNumber) {
        UUID userId = insertUser(email);
        UUID originStationId = insertStation(stationCodePrefix + "O", "Origin", "Ha Noi");
        UUID destinationStationId =
                insertStation(stationCodePrefix + "D", "Destination", "Da Nang");
        UUID trainId = insertTrain(stationCodePrefix + "T", "Test Express", 1);
        UUID coachId = insertCoach(trainId, 1, 1);
        UUID seatId = insertSeat(coachId, seatNumber);
        UUID routeTemplateId = insertRouteTemplate(originStationId, destinationStationId, 500000);
        UUID scheduledTripId = insertScheduledTrip(routeTemplateId, trainId);
        insertTripSeatAvailability(scheduledTripId, seatId);
        return new TestBookingData(userId, scheduledTripId, seatId);
    }

    private UUID insertUser(String email) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, full_name, role, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                id,
                email,
                "hash",
                "Test User",
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

    private void insertTripSeatAvailability(UUID scheduledTripId, UUID seatId) {
        jdbcTemplate.update(
                "INSERT INTO trip_seat_availability (scheduled_trip_id, seat_id, status, version) VALUES (?, ?, 'AVAILABLE', 0)",
                scheduledTripId,
                seatId);
    }

    private void cleanupTestData() {
        jdbcTemplate.update(
                "DELETE FROM trip_seat_availability WHERE scheduled_trip_id IN (SELECT st.id FROM scheduled_trips st JOIN route_templates rt ON st.route_template_id = rt.id JOIN stations s ON rt.origin_station_id = s.id WHERE s.code LIKE 'TESTBK%')");
        jdbcTemplate.update(
                "DELETE FROM bookings WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'test-booking-%')");
        jdbcTemplate.update(
                "DELETE FROM scheduled_trips WHERE id IN (SELECT st.id FROM scheduled_trips st JOIN route_templates rt ON st.route_template_id = rt.id JOIN stations s ON rt.origin_station_id = s.id WHERE s.code LIKE 'TESTBK%')");
        jdbcTemplate.update(
                "DELETE FROM route_templates WHERE origin_station_id IN (SELECT id FROM stations WHERE code LIKE 'TESTBK%')");
        jdbcTemplate.update(
                "DELETE FROM seats WHERE coach_id IN (SELECT id FROM coaches WHERE train_id IN (SELECT id FROM trains WHERE train_number LIKE 'TESTBK%'))");
        jdbcTemplate.update(
                "DELETE FROM coaches WHERE train_id IN (SELECT id FROM trains WHERE train_number LIKE 'TESTBK%')");
        jdbcTemplate.update("DELETE FROM trains WHERE train_number LIKE 'TESTBK%'");
        jdbcTemplate.update("DELETE FROM stations WHERE code LIKE 'TESTBK%'");
        jdbcTemplate.update("DELETE FROM users WHERE email LIKE 'test-booking-%'");
    }

    private interface CommandFactory {
        CreateBookingCommand create(int index);
    }

    private record TestBookingData(UUID userId, UUID scheduledTripId, UUID seatId) {}
}
