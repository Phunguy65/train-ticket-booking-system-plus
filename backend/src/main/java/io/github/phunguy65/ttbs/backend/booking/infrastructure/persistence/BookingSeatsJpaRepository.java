package io.github.phunguy65.ttbs.backend.booking.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface BookingSeatsJpaRepository extends JpaRepository<BookingSeatsEntity, BookingSeatsId> {

    List<BookingSeatsEntity> findByIdBookingId(UUID bookingId);
}
