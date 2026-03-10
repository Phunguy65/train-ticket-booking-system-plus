package io.github.phunguy65.ttbs.backend.payment.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.payment.domain.model.Payment;
import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentId;
import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentStatus;
import io.github.phunguy65.ttbs.backend.payment.domain.repository.PaymentRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@Import({PaymentRepositoryAdapter.class, PaymentEntityMapper.class})
@TestPropertySource(properties = "spring.modulith.detection.disabled=true")
class PaymentRepositoryAdapterTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID userId;
    private UUID bookingId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        bookingId = UUID.randomUUID();

        // Insert prerequisite user (FK constraint)
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, full_name, role, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                userId,
                "test-" + userId + "@example.com",
                "hash",
                "Test User",
                "CUSTOMER",
                java.sql.Timestamp.from(Instant.now()),
                java.sql.Timestamp.from(Instant.now()));

        // Insert prerequisite booking (FK constraint)
        UUID routeId = UUID.randomUUID();
        UUID trainId = UUID.randomUUID();
        UUID originId = UUID.randomUUID();
        UUID destId = UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO trains (id, train_number, name, total_seats, created_at) VALUES (?, ?, ?, ?, ?)",
                trainId,
                "T-" + trainId.toString().substring(0, 8),
                "Test Train",
                100,
                java.sql.Timestamp.from(Instant.now()));
        jdbcTemplate.update(
                "INSERT INTO stations (id, code, name, city, created_at) VALUES (?, ?, ?, ?, ?)",
                originId,
                "ORI",
                "Origin",
                "City A",
                java.sql.Timestamp.from(Instant.now()));
        jdbcTemplate.update(
                "INSERT INTO stations (id, code, name, city, created_at) VALUES (?, ?, ?, ?, ?)",
                destId,
                "DST",
                "Destination",
                "City B",
                java.sql.Timestamp.from(Instant.now()));
        jdbcTemplate.update(
                "INSERT INTO routes (id, train_id, origin_station_id, destination_station_id, departure_time, arrival_time, base_price, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                routeId,
                trainId,
                originId,
                destId,
                java.sql.Timestamp.from(Instant.now().plusSeconds(3600)),
                java.sql.Timestamp.from(Instant.now().plusSeconds(7200)),
                100000L,
                "SCHEDULED",
                java.sql.Timestamp.from(Instant.now()));
        jdbcTemplate.update(
                "INSERT INTO bookings (id, user_id, route_id, passenger_name, passenger_email, total_price, currency, status, idempotency_key, payment_deadline, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                bookingId,
                userId,
                routeId,
                "John Doe",
                "john@example.com",
                100000L,
                "VND",
                "HELD",
                "idem-key-1",
                java.sql.Timestamp.from(Instant.now().plusSeconds(900)),
                java.sql.Timestamp.from(Instant.now()));
    }

    private Payment newPendingPayment() {
        return Payment.create(
                PaymentId.generate(),
                BookingId.of(bookingId),
                UserId.of(userId),
                Money.vnd(100_000L),
                "cs_test_" + UUID.randomUUID().toString().substring(0, 8),
                "https://checkout.stripe.com/pay/cs_test_123");
    }

    @Test
    void save_andFindByBookingId_shouldRoundTrip() {
        Payment payment = newPendingPayment();
        paymentRepository.save(payment);

        Optional<Payment> found = paymentRepository.findByBookingId(BookingId.of(bookingId));

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(found.get().getBookingId().value()).isEqualTo(bookingId);
    }

    @Test
    void findByCheckoutSessionId_shouldReturnPayment() {
        Payment payment = newPendingPayment();
        paymentRepository.save(payment);

        Optional<Payment> found =
                paymentRepository.findByCheckoutSessionId(payment.getCheckoutSessionId());

        assertThat(found).isPresent();
        assertThat(found.get().getPaymentId()).isEqualTo(payment.getPaymentId());
    }

    @Test
    void findByStripeEventId_shouldReturnPayment() {
        Payment payment = newPendingPayment();
        payment.markPaid("pi_test_456", "evt_test_789");
        paymentRepository.save(payment);

        Optional<Payment> found = paymentRepository.findByStripeEventId("evt_test_789");

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void findByBookingId_whenNotFound_shouldReturnEmpty() {
        Optional<Payment> found =
                paymentRepository.findByBookingId(BookingId.of(UUID.randomUUID()));

        assertThat(found).isEmpty();
    }
}
