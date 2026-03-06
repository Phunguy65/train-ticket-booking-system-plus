package io.github.phunguy65.ttbs.backend.booking.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.booking.domain.model.UserId;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import java.time.Instant;
import java.util.List;
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
@Import({BookingRepositoryAdapter.class, BookingEntityMapper.class})
@TestPropertySource(properties = "spring.modulith.detection.disabled=true")
class BookingRepositoryAdapterTest {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID userId;
    private UUID routeId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        routeId = UUID.randomUUID();

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

        // Insert prerequisite train + station + route (FK constraints)
        UUID trainId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO trains (id, train_number, name, total_seats, created_at) VALUES (?, ?, ?, ?, ?)",
                trainId,
                "T-" + trainId.toString().substring(0, 8),
                "Test Train",
                100,
                java.sql.Timestamp.from(Instant.now()));

        UUID originId = UUID.randomUUID();
        UUID destId = UUID.randomUUID();
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
                "INSERT INTO routes (id, train_id, origin_station_id, destination_station_id, departure_time, arrival_time, base_price, status, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                routeId,
                trainId,
                originId,
                destId,
                java.sql.Timestamp.from(Instant.now().plusSeconds(3600)),
                java.sql.Timestamp.from(Instant.now().plusSeconds(7200)),
                100000L,
                "SCHEDULED",
                java.sql.Timestamp.from(Instant.now()));
    }

    private Booking newHeldBooking(String idempotencyKey) {
        return Booking.create(
                BookingId.of(UUID.randomUUID()),
                UserId.of(userId),
                RouteId.of(routeId),
                "John Doe",
                "john@example.com",
                null,
                Money.vnd(100_000L),
                "VND",
                idempotencyKey,
                Instant.now().plusSeconds(900));
    }

    @Test
    void save_andFindById_shouldRoundTrip() {
        Booking booking = newHeldBooking("idem-1");
        bookingRepository.save(booking);

        Optional<Booking> found = bookingRepository.findById(booking.getBookingId());

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(BookingStatus.HELD);
        assertThat(found.get().getPassengerName()).isEqualTo("John Doe");
    }

    @Test
    void findByIdempotencyKey_shouldReturnBooking() {
        Booking booking = newHeldBooking("idem-unique-key");
        bookingRepository.save(booking);

        Optional<Booking> found = bookingRepository.findByIdempotencyKey("idem-unique-key");

        assertThat(found).isPresent();
        assertThat(found.get().getBookingId()).isEqualTo(booking.getBookingId());
    }

    @Test
    void findByIdempotencyKey_whenNotFound_shouldReturnEmpty() {
        Optional<Booking> found = bookingRepository.findByIdempotencyKey("nonexistent");

        assertThat(found).isEmpty();
    }

    @Test
    void findActiveHoldByUserAndRoute_shouldReturnHeldBooking() {
        Booking booking = newHeldBooking("idem-hold");
        bookingRepository.save(booking);

        Optional<Booking> found = bookingRepository.findActiveHoldByUserAndRoute(
                UserId.of(userId), RouteId.of(routeId));

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(BookingStatus.HELD);
    }

    @Test
    void findExpiredHeldBookings_shouldReturnExpiredBookings() {
        Booking expired = Booking.create(
                BookingId.of(UUID.randomUUID()),
                UserId.of(userId),
                RouteId.of(routeId),
                "Jane Doe",
                "jane@example.com",
                null,
                Money.vnd(100_000L),
                "VND",
                "idem-expired",
                Instant.now().minusSeconds(60)); // already expired
        bookingRepository.save(expired);

        List<Booking> found = bookingRepository.findExpiredHeldBookings(Instant.now());

        assertThat(found).hasSize(1);
        assertThat(found.getFirst().getIdempotencyKey()).isEqualTo("idem-expired");
    }
}
