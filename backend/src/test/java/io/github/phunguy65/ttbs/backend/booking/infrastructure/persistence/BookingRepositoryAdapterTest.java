package io.github.phunguy65.ttbs.backend.booking.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({BookingRepositoryAdapter.class, BookingEntityMapper.class})
@org.springframework.test.context.TestPropertySource(
        properties = "spring.modulith.detection.disabled=true")
class BookingRepositoryAdapterTest {

    @Autowired
    private BookingRepository bookingRepository;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ROUTE_ID = UUID.randomUUID();
    private static final UUID SEAT_ID = UUID.randomUUID();

    @Test
    void save_shouldPersistBooking() {
        Booking booking = Booking.create(
                USER_ID, ROUTE_ID, SEAT_ID, new BigDecimal("200000.00"), "VND", "idem-key-save");

        Booking saved = bookingRepository.save(booking);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId().value()).isEqualTo(USER_ID);
        assertThat(saved.getStatus()).isEqualTo(BookingStatus.PENDING);
    }

    @Test
    void findById_shouldReturnCorrectDomainModel() {
        Booking booking = Booking.create(
                USER_ID, ROUTE_ID, SEAT_ID, new BigDecimal("150000.00"), "VND", "idem-key-find");
        Booking saved = bookingRepository.save(booking);

        Optional<Booking> found = bookingRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getUserId().value()).isEqualTo(USER_ID);
        assertThat(found.get().getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(found.get().getDomainEvents()).isEmpty();
    }

    @Test
    void save_thenFindById_shouldPreserveAllFields() {
        BigDecimal price = new BigDecimal("300000.00");
        Booking booking =
                Booking.create(USER_ID, ROUTE_ID, SEAT_ID, price, "VND", "idem-key-roundtrip");
        Booking saved = bookingRepository.save(booking);

        Optional<Booking> found = bookingRepository.findById(saved.getId());

        assertThat(found).isPresent();
        Booking retrieved = found.get();
        assertThat(retrieved.getUserId().value()).isEqualTo(USER_ID);
        assertThat(retrieved.getRouteId().value()).isEqualTo(ROUTE_ID);
        assertThat(retrieved.getSeatId().value()).isEqualTo(SEAT_ID);
        assertThat(retrieved.getTotalPrice()).isEqualByComparingTo(price);
        assertThat(retrieved.getCurrency()).isEqualTo("VND");
        assertThat(retrieved.getIdempotencyKey()).isEqualTo("idem-key-roundtrip");
        assertThat(retrieved.getStatus()).isEqualTo(BookingStatus.PENDING);
    }

    @Test
    void findById_withUnknownId_shouldReturnEmpty() {
        Optional<Booking> found = bookingRepository.findById(BookingId.of(UUID.randomUUID()));

        assertThat(found).isEmpty();
    }
}
