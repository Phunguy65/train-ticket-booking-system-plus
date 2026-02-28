package io.github.phunguy65.ttbs.backend.booking.application.port;

import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import java.util.List;

/**
 * Cross-module port for validating seat constraints before deletion.
 *
 * <p>Exposed via the {@code booking::port} named interface — allows the {@code train} module to
 * check booking dependencies without coupling to booking JPA internals.
 */
public interface SeatValidationPort {

    /**
     * Returns {@code true} if there are any historical bookings (HELD, CONFIRMED, or CANCELLED)
     * that reference the given seat.
     *
     * @param seatId the seat to check
     * @return {@code true} if deletion is blocked
     */
    boolean hasBookingHistoryForSeat(SeatId seatId);

    /**
     * Returns {@code true} if ANY of the given seats have booking history.
     *
     * @param seatIds the seats to check
     * @return {@code true} if deletion is blocked for any seat
     */
    boolean hasBookingHistoryForAnySeats(List<SeatId> seatIds);
}
