package io.github.phunguy65.ttbs.backend.booking.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookedSeat;
import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class BookingEntityMapper {

    /**
     * Maps a {@link BookingEntity} (with its {@code seats} collection loaded) to a domain
     * {@link Booking}. Uses {@link Booking#reconstitute} — does NOT register domain events.
     */
    Booking toDomain(BookingEntity entity) {
        List<BookedSeat> bookedSeats = entity.getSeats().stream()
                .map(seat -> BookedSeat.of(SeatId.of(seat.getSeatId()), seat.getPriceAtBooking()))
                .toList();

        return Booking.reconstitute(
                entity.getId(),
                entity.getUserId(),
                entity.getRouteId(),
                bookedSeats,
                entity.getTotalPrice(),
                entity.getCurrency(),
                entity.getIdempotencyKey(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getCheckoutSessionId(),
                entity.getPassengerName(),
                entity.getPassengerEmail(),
                entity.getPassengerPhone());
    }

    /**
     * Maps a domain {@link Booking} to a {@link BookingEntity}, including its
     * {@link BookingSeatsEntity} children.
     */
    BookingEntity toEntity(Booking booking) {
        BookingEntity entity = new BookingEntity();
        entity.setId(booking.getId().value());
        entity.setUserId(booking.getUserId().value());
        entity.setRouteId(booking.getRouteId().value());
        entity.setTotalPrice(booking.getTotalPrice());
        entity.setCurrency(booking.getCurrency());
        entity.setStatus(booking.getStatus());
        entity.setIdempotencyKey(booking.getIdempotencyKey());
        entity.setCreatedAt(booking.getCreatedAt());
        entity.setCheckoutSessionId(booking.getCheckoutSessionId());
        entity.setPassengerName(booking.getPassengerName());
        entity.setPassengerEmail(booking.getPassengerEmail());
        entity.setPassengerPhone(booking.getPassengerPhone());

        booking.getBookedSeats().forEach(bs -> {
            BookingSeatsEntity seatEntity = new BookingSeatsEntity(
                    booking.getId().value(), bs.seatId().value(), bs.unitPrice());
            entity.addSeat(seatEntity);
        });

        return entity;
    }
}
