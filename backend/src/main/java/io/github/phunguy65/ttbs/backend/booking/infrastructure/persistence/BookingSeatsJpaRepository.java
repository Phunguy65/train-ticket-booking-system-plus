package io.github.phunguy65.ttbs.backend.booking.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface BookingSeatsJpaRepository extends JpaRepository<BookingSeatsEntity, BookingSeatsId> {

    List<BookingSeatsEntity> findByIdBookingId(UUID bookingId);

    @Query("SELECT COUNT(bs) > 0 FROM BookingSeatsEntity bs WHERE bs.id.seatId = :seatId")
    boolean existsBySeatId(@Param("seatId") UUID seatId);
}
