package io.github.phunguy65.ttbs.backend.train.application.port.validation;

import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import java.util.List;

/**
 * Cross-module port for validating seat constraints before deletion.
 *
 * <p>Exposed via the {@code train::validation} named interface — allows the {@code booking} module
 * to implement this port without creating a circular dependency between {@code train} and
 * {@code booking}.
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
