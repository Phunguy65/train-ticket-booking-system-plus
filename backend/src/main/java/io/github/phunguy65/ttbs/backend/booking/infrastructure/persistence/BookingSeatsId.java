package io.github.phunguy65.ttbs.backend.booking.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
class BookingSeatsId implements Serializable {

    @Column(name = "booking_id", nullable = false, updatable = false)
    private UUID bookingId;

    @Column(name = "seat_id", nullable = false, updatable = false)
    private UUID seatId;

    protected BookingSeatsId() {}

    BookingSeatsId(UUID bookingId, UUID seatId) {
        this.bookingId = bookingId;
        this.seatId = seatId;
    }

    UUID getBookingId() {
        return bookingId;
    }

    UUID getSeatId() {
        return seatId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookingSeatsId that)) return false;
        return Objects.equals(bookingId, that.bookingId) && Objects.equals(seatId, that.seatId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bookingId, seatId);
    }
}
