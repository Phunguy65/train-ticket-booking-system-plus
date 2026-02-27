package io.github.phunguy65.ttbs.backend.booking.infrastructure.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * JPA entity mapping the {@code booking_seats} join table.
 * Stores a price snapshot per seat at the time of hold creation.
 */
@Entity
@Table(name = "booking_seats")
class BookingSeatsEntity {

    @EmbeddedId
    private BookingSeatsId id;

    @Column(name = "price_at_booking", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAtBooking;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("bookingId")
    @JoinColumn(name = "booking_id")
    private BookingEntity booking;

    protected BookingSeatsEntity() {}

    BookingSeatsEntity(UUID bookingId, UUID seatId, BigDecimal priceAtBooking) {
        this.id = new BookingSeatsId(bookingId, seatId);
        this.priceAtBooking = priceAtBooking;
    }

    BookingSeatsId getId() {
        return id;
    }

    void setId(BookingSeatsId id) {
        this.id = id;
    }

    UUID getSeatId() {
        return id.getSeatId();
    }

    BigDecimal getPriceAtBooking() {
        return priceAtBooking;
    }

    void setPriceAtBooking(BigDecimal priceAtBooking) {
        this.priceAtBooking = priceAtBooking;
    }

    BookingEntity getBooking() {
        return booking;
    }

    void setBooking(BookingEntity booking) {
        this.booking = booking;
    }
}
