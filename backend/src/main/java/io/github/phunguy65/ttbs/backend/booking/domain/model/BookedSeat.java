package io.github.phunguy65.ttbs.backend.booking.domain.model;

import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import java.math.BigDecimal;

/**
 * Value object representing a seat that has been booked (held) in a booking, including
 * the price snapshot at the time of hold creation.
 *
 * <p>Immutable — all fields are set at construction time and never mutated.
 * Price snapshotting ensures that subsequent changes to route base price
 * cannot affect in-flight holds.
 */
public record BookedSeat(SeatId seatId, BigDecimal unitPrice) {

    public BookedSeat {
        if (seatId == null) throw new IllegalArgumentException("seatId must not be null");
        if (unitPrice == null) throw new IllegalArgumentException("unitPrice must not be null");
    }

    public static BookedSeat of(SeatId seatId, BigDecimal unitPrice) {
        return new BookedSeat(seatId, unitPrice);
    }
}
