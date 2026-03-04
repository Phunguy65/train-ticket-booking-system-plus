package io.github.phunguy65.ttbs.backend.booking.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookedSeat;
import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@Import({BookingRepositoryAdapter.class, BookingEntityMapper.class})
@TestPropertySource(properties = "spring.modulith.detection.disabled=true")
class BookingRepositoryAdapterTest {

    @Autowired
    private BookingRepository bookingRepository;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ROUTE_ID = UUID.randomUUID();
    private static final UUID SEAT_ID = UUID.randomUUID();

    private static List<BookedSeat> oneEconomySeat() {
        return List.of(BookedSeat.of(SeatId.of(SEAT_ID), new BigDecimal("200000.00")));
    }

    private static Booking createHold(String idemKey) {
        return Booking.createHold(
                USER_ID,
                ROUTE_ID,
                oneEconomySeat(),
                new BigDecimal("200000.00"),
                "VND",
                null,
                idemKey,
                "Test Passenger",
                "test@example.com",
                null);
    }

    @Test
    void save_shouldPersistBooking() {
        Booking booking = createHold("idem-key-save");

        Booking saved = bookingRepository.save(booking);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId().value()).isEqualTo(USER_ID);
        assertThat(saved.getStatus()).isEqualTo(BookingStatus.HELD);
    }

    @Test
    void findById_shouldReturnCorrectDomainModel() {
        Booking booking = createHold("idem-key-find");
        Booking saved = bookingRepository.save(booking);

        Optional<Booking> found = bookingRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getUserId().value()).isEqualTo(USER_ID);
        assertThat(found.get().getStatus()).isEqualTo(BookingStatus.HELD);
        assertThat(found.get().getDomainEvents()).isEmpty();
    }

    @Test
    void save_thenFindById_shouldPreserveBookedSeats() {
        Booking booking = createHold("idem-key-roundtrip");
        Booking saved = bookingRepository.save(booking);

        Optional<Booking> found = bookingRepository.findById(saved.getId());

        assertThat(found).isPresent();
        Booking retrieved = found.get();
        assertThat(retrieved.getBookedSeats()).hasSize(1);
        assertThat(retrieved.getBookedSeats().getFirst().seatId().value()).isEqualTo(SEAT_ID);
        assertThat(retrieved.getBookedSeats().getFirst().unitPrice())
                .isEqualByComparingTo("200000.00");
    }

    @Test
    void findById_withUnknownId_shouldReturnEmpty() {
        Optional<Booking> found = bookingRepository.findById(BookingId.of(UUID.randomUUID()));

        assertThat(found).isEmpty();
    }

    @Test
    void findByIdempotencyKey_shouldReturnBooking() {
        Booking booking = createHold("idem-key-lookup");
        bookingRepository.save(booking);

        Optional<Booking> found = bookingRepository.findByIdempotencyKey("idem-key-lookup");

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(BookingStatus.HELD);
    }

    @Test
    void findActiveHoldByUserIdAndRouteId_whenHoldExists_shouldReturn() {
        Booking booking = createHold("idem-active-hold");
        bookingRepository.save(booking);

        Optional<Booking> found = bookingRepository.findActiveHoldByUserIdAndRouteId(
                UserId.of(USER_ID), RouteId.of(ROUTE_ID));

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(BookingStatus.HELD);
    }

    @Test
    void findActiveHoldByUserIdAndRouteId_whenNoHold_shouldReturnEmpty() {
        Optional<Booking> found = bookingRepository.findActiveHoldByUserIdAndRouteId(
                UserId.of(UUID.randomUUID()), RouteId.of(UUID.randomUUID()));

        assertThat(found).isEmpty();
    }
}
