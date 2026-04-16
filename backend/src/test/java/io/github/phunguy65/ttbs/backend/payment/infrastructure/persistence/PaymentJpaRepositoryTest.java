package io.github.phunguy65.ttbs.backend.payment.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.phunguy65.ttbs.backend.TestContainerConfiguration;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainerConfiguration.class)
@Transactional
class PaymentJpaRepositoryTest {

    private static final Instant CREATED_AT = Instant.parse("2026-04-01T09:00:00Z");
    private static final UUID USER_ID = UUID.fromString("31111111-1111-1111-1111-111111111111");
    private static final UUID ORIGIN_ID = UUID.fromString("31111111-1111-1111-1111-111111111112");
    private static final UUID DESTINATION_ID =
            UUID.fromString("31111111-1111-1111-1111-111111111113");
    private static final UUID ROUTE_ID = UUID.fromString("31111111-1111-1111-1111-111111111114");
    private static final UUID TRIP_ID = UUID.fromString("31111111-1111-1111-1111-111111111115");
    private static final UUID BOOKING_ID = UUID.fromString("31111111-1111-1111-1111-111111111116");
    private static final UUID PAYMENT_ID = UUID.fromString("31111111-1111-1111-1111-111111111117");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PaymentJpaRepository paymentJpaRepository;

    @Test
    void findSummaryByBookingIdReturnsExpandedProjection() {
        insertGraph();

        PaymentSummaryView payment =
                paymentJpaRepository.findPaymentSummaryByBookingId(BOOKING_ID).orElseThrow();

        assertThat(payment.getId()).isEqualTo(PAYMENT_ID);
        assertThat(payment.getBookingId()).isEqualTo(BOOKING_ID);
        assertThat(payment.getUserId()).isEqualTo(USER_ID);
        assertThat(payment.getStatus()).isEqualTo("PENDING");
        assertThat(payment.getCheckoutUrl()).isNull();
        assertThat(payment.getAmount()).isEqualTo(450_000);
        assertThat(payment.getCurrency()).isEqualTo("VND");
        assertThat(payment.getStripePaymentIntentId()).isEqualTo("pi_123");
        assertThat(payment.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    private void insertGraph() {
        insertUser();
        insertStations();
        insertRouteTemplate();
        insertTrip();
        insertBooking();
        insertPayment();
    }

    private void insertUser() {
        jdbcTemplate.update(
                """
                INSERT INTO users (id, email, password_hash, full_name, role, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                USER_ID,
                "payment-projection@example.com",
                "test-password-hash",
                "Test User",
                "CUSTOMER",
                Timestamp.from(CREATED_AT),
                Timestamp.from(CREATED_AT));
    }

    private void insertStations() {
        jdbcTemplate.update(
                "INSERT INTO stations (id, code, name, city, created_at) VALUES (?, ?, ?, ?, ?)",
                ORIGIN_ID,
                "PSGN",
                "Sai Gon",
                "Ho Chi Minh",
                Timestamp.from(CREATED_AT));
        jdbcTemplate.update(
                "INSERT INTO stations (id, code, name, city, created_at) VALUES (?, ?, ?, ?, ?)",
                DESTINATION_ID,
                "PDAD",
                "Da Nang",
                "Da Nang",
                Timestamp.from(CREATED_AT));
    }

    private void insertRouteTemplate() {
        jdbcTemplate.update(
                """
                INSERT INTO route_templates (id, origin_station_id, destination_station_id, base_price, created_at)
                VALUES (?, ?, ?, ?, ?)
                """, ROUTE_ID, ORIGIN_ID, DESTINATION_ID, 450_000, Timestamp.from(CREATED_AT));
    }

    private void insertTrip() {
        jdbcTemplate.update(
                """
                INSERT INTO scheduled_trips (id, route_template_id, departure_time, arrival_time, status, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                TRIP_ID,
                ROUTE_ID,
                Timestamp.from(Instant.parse("2026-05-01T08:00:00Z")),
                Timestamp.from(Instant.parse("2026-05-01T12:00:00Z")),
                "SCHEDULED",
                Timestamp.from(CREATED_AT));
    }

    private void insertBooking() {
        jdbcTemplate.update(
                """
                INSERT INTO bookings (id, user_id, scheduled_trip_id, user_info_snapshot, total_price, currency, status, idempotency_key, payment_deadline, created_at)
                VALUES (?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, ?, ?)
                """,
                BOOKING_ID,
                USER_ID,
                TRIP_ID,
                "{" + "\"fullName\":\"Nguyen Van A\","
                        + "\"email\":\"a@example.com\","
                        + "\"phone\":\"0900000000\","
                        + "\"dateOfBirth\":null,"
                        + "\"gender\":\"MALE\","
                        + "\"idDocumentNumber\":\"0123456789\","
                        + "\"addressLine\":\"123 Test Street\"}",
                450_000,
                "VND",
                "HELD",
                "idem-payment",
                Timestamp.from(Instant.parse("2026-05-01T09:15:00Z")),
                Timestamp.from(CREATED_AT));
    }

    private void insertPayment() {
        jdbcTemplate.update(
                """
                INSERT INTO payments (id, booking_id, user_id, checkout_session_id, amount, currency, status, stripe_payment_intent_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                PAYMENT_ID,
                BOOKING_ID,
                USER_ID,
                "cs_test_123",
                450_000,
                "VND",
                "PENDING",
                "pi_123",
                Timestamp.from(CREATED_AT),
                Timestamp.from(CREATED_AT));
    }
}
