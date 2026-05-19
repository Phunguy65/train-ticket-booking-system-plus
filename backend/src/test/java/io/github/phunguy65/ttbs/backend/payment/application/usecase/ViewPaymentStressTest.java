package io.github.phunguy65.ttbs.backend.payment.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.phunguy65.ttbs.backend.TestContainerConfiguration;
import io.github.phunguy65.ttbs.backend.payment.application.query.GetPaymentByBookingIdQuery;
import io.github.phunguy65.ttbs.backend.payment.application.query.GetPaymentByIdQuery;
import io.github.phunguy65.ttbs.backend.payment.application.response.PaymentDetailResponse;
import io.github.phunguy65.ttbs.backend.payment.application.response.PaymentResponse;
import io.github.phunguy65.ttbs.backend.payment.domain.error.PaymentError;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import java.sql.Timestamp;
import java.time.Instant;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainerConfiguration.class)
@DisplayName("ViewPayment stress")
class ViewPaymentStressTest {

    @Autowired
    private GetPaymentByIdUseCase getPaymentByIdUseCase;

    @Autowired
    private GetPaymentByBookingIdUseCase getPaymentByBookingIdUseCase;

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
    @DisplayName("handles 50 concurrent getPaymentById requests with consistent results")
    void getPaymentById_handles50ConcurrentRequestsWithConsistentResults() throws Exception {
        TestViewPaymentData data = insertViewPaymentData("TESTVP1", "test-viewpay-id@example.com");
        GetPaymentByIdQuery query = new GetPaymentByIdQuery(data.paymentId(), data.userId());

        List<Result<PaymentDetailResponse, PaymentError>> results =
                runConcurrentPaymentDetails(50, query);

        assertThat(results).hasSize(50);
        assertThat(results).allMatch(Result::isSuccess);
        List<PaymentDetailResponse> responses = results.stream()
                .map(r -> ((Result.Success<PaymentDetailResponse, PaymentError>) r).value())
                .toList();
        assertThat(responses.stream().map(PaymentDetailResponse::paymentId))
                .allMatch(id -> id.equals(data.paymentId()));
        assertThat(responses.stream().map(PaymentDetailResponse::bookingId))
                .allMatch(id -> id.equals(data.bookingId()));
        assertThat(responses.stream().map(PaymentDetailResponse::status).distinct())
                .hasSize(1);
        assertThat(responses.stream().map(PaymentDetailResponse::amount).distinct())
                .hasSize(1);
        assertThat(responses.stream().map(PaymentDetailResponse::booking).distinct())
                .hasSize(1);
    }

    @Test
    @DisplayName("handles 50 concurrent getPaymentByBookingId requests with consistent results")
    void getPaymentByBookingId_handles50ConcurrentRequestsWithConsistentResults() throws Exception {
        TestViewPaymentData data =
                insertViewPaymentData("TESTVP2", "test-viewpay-booking@example.com");
        GetPaymentByBookingIdQuery query =
                new GetPaymentByBookingIdQuery(data.bookingId(), data.userId());

        List<Result<PaymentResponse, PaymentError>> results =
                runConcurrentBookingPayments(50, query);

        assertThat(results).hasSize(50);
        assertThat(results).allMatch(Result::isSuccess);
        List<PaymentResponse> responses = results.stream()
                .map(r -> ((Result.Success<PaymentResponse, PaymentError>) r).value())
                .toList();
        assertThat(responses.stream().map(PaymentResponse::paymentId))
                .allMatch(id -> id.equals(data.paymentId()));
        assertThat(responses.stream().map(PaymentResponse::bookingId))
                .allMatch(id -> id.equals(data.bookingId()));
        assertThat(responses.stream().distinct()).hasSize(1);
    }

    private List<Result<PaymentDetailResponse, PaymentError>> runConcurrentPaymentDetails(
            int threadCount, GetPaymentByIdQuery query) throws Exception {
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            List<Future<Result<PaymentDetailResponse, PaymentError>>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(paymentDetailTask(query, ready, start)));
            }
            ready.await(10, TimeUnit.SECONDS);
            start.countDown();
            List<Result<PaymentDetailResponse, PaymentError>> results = new ArrayList<>();
            for (Future<Result<PaymentDetailResponse, PaymentError>> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private List<Result<PaymentResponse, PaymentError>> runConcurrentBookingPayments(
            int threadCount, GetPaymentByBookingIdQuery query) throws Exception {
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            List<Future<Result<PaymentResponse, PaymentError>>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(bookingPaymentTask(query, ready, start)));
            }
            ready.await(10, TimeUnit.SECONDS);
            start.countDown();
            List<Result<PaymentResponse, PaymentError>> results = new ArrayList<>();
            for (Future<Result<PaymentResponse, PaymentError>> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private Callable<Result<PaymentDetailResponse, PaymentError>> paymentDetailTask(
            GetPaymentByIdQuery query, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            start.await(10, TimeUnit.SECONDS);
            return getPaymentByIdUseCase.execute(query);
        };
    }

    private Callable<Result<PaymentResponse, PaymentError>> bookingPaymentTask(
            GetPaymentByBookingIdQuery query, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            start.await(10, TimeUnit.SECONDS);
            return getPaymentByBookingIdUseCase.execute(query);
        };
    }

    private TestViewPaymentData insertViewPaymentData(String stationCodePrefix, String email) {
        UUID userId = insertUser(email);
        UUID originStationId = insertStation(stationCodePrefix + "O", "Origin", "Ha Noi");
        UUID destinationStationId =
                insertStation(stationCodePrefix + "D", "Destination", "Da Nang");
        UUID trainId = insertTrain(stationCodePrefix + "T", "View Payment Express", 1);
        UUID coachId = insertCoach(trainId, 1, 1);
        UUID seatId = insertSeat(coachId, "1A");
        UUID routeTemplateId = insertRouteTemplate(originStationId, destinationStationId, 500000);
        UUID scheduledTripId = insertScheduledTrip(routeTemplateId, trainId);
        insertTripSeatAvailability(scheduledTripId, seatId);
        UUID bookingId = insertBooking(userId, scheduledTripId, seatId);
        UUID paymentId = insertPayment(bookingId, userId);
        holdSeatForBooking(scheduledTripId, seatId, bookingId);
        return new TestViewPaymentData(userId, bookingId, paymentId);
    }

    private UUID insertUser(String email) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, full_name, role, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                id,
                email,
                "hash",
                "Test View Payment User",
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

    private UUID insertBooking(UUID userId, UUID scheduledTripId, UUID seatId) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO bookings (id, user_id, scheduled_trip_id, user_info_snapshot, passengers_snapshot, total_price, currency, status, idempotency_key, payment_deadline, created_at) VALUES (?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), ?, ?, ?, ?, ?, ?)",
                id,
                userId,
                scheduledTripId,
                userInfoSnapshot(),
                passengerSnapshot(seatId),
                500000,
                "VND",
                "HELD",
                "view-payment-" + id,
                Timestamp.from(Instant.parse("2026-08-01T07:45:00Z")),
                Timestamp.from(Instant.parse("2026-04-01T09:00:00Z")));
        return id;
    }

    private UUID insertPayment(UUID bookingId, UUID userId) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO payments (id, booking_id, user_id, checkout_session_id, status, checkout_url, amount, currency, stripe_payment_intent_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id,
                bookingId,
                userId,
                "cs_view_payment_" + bookingId,
                "PENDING",
                "https://checkout.test/view-payment",
                500000,
                "VND",
                "pi_view_payment",
                Timestamp.from(Instant.parse("2026-04-01T10:00:00Z")),
                Timestamp.from(Instant.parse("2026-04-01T10:00:00Z")));
        return id;
    }

    private void holdSeatForBooking(UUID scheduledTripId, UUID seatId, UUID bookingId) {
        jdbcTemplate.update(
                "UPDATE trip_seat_availability SET status = 'HELD', booking_id = ?, version = 1 WHERE scheduled_trip_id = ? AND seat_id = ?",
                bookingId,
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
                "DELETE FROM payments WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'test-viewpay-%')");
        jdbcTemplate.update(
                "DELETE FROM trip_seat_availability WHERE scheduled_trip_id IN (SELECT st.id FROM scheduled_trips st JOIN route_templates rt ON st.route_template_id = rt.id JOIN stations s ON rt.origin_station_id = s.id WHERE s.code LIKE 'TESTVP%')");
        jdbcTemplate.update(
                "DELETE FROM bookings WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'test-viewpay-%')");
        jdbcTemplate.update(
                "DELETE FROM scheduled_trips WHERE id IN (SELECT st.id FROM scheduled_trips st JOIN route_templates rt ON st.route_template_id = rt.id JOIN stations s ON rt.origin_station_id = s.id WHERE s.code LIKE 'TESTVP%')");
        jdbcTemplate.update(
                "DELETE FROM route_templates WHERE origin_station_id IN (SELECT id FROM stations WHERE code LIKE 'TESTVP%')");
        jdbcTemplate.update(
                "DELETE FROM seats WHERE coach_id IN (SELECT id FROM coaches WHERE train_id IN (SELECT id FROM trains WHERE train_number LIKE 'TESTVP%'))");
        jdbcTemplate.update(
                "DELETE FROM coaches WHERE train_id IN (SELECT id FROM trains WHERE train_number LIKE 'TESTVP%')");
        jdbcTemplate.update("DELETE FROM trains WHERE train_number LIKE 'TESTVP%'");
        jdbcTemplate.update("DELETE FROM stations WHERE code LIKE 'TESTVP%'");
        jdbcTemplate.update("DELETE FROM users WHERE email LIKE 'test-viewpay-%'");
    }

    private record TestViewPaymentData(UUID userId, UUID bookingId, UUID paymentId) {}
}
