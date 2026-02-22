package io.github.phunguy65.ttbs.backend.booking.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
class BookingEntityMapper {

    private static final String DEFAULT_PASSENGER_NAME = "Unknown";
    private static final String DEFAULT_PASSENGER_EMAIL = "unknown@example.com";
    private static final String DEFAULT_PAYMENT_STATUS = "PENDING";

    Booking toDomain(BookingEntity entity) {
        return Booking.reconstitute(
                entity.getId(),
                entity.getUserId(),
                entity.getRouteId(),
                entity.getSeatId(),
                entity.getTotalPrice(),
                entity.getCurrency(),
                entity.getIdempotencyKey(),
                entity.getStatus(),
                entity.getCreatedAt() != null ? entity.getCreatedAt() : Instant.now());
    }

    BookingEntity toEntity(Booking booking) {
        BookingEntity entity = new BookingEntity();
        entity.setId(booking.getId().value());
        entity.setUserId(booking.getUserId().value());
        entity.setRouteId(booking.getRouteId().value());
        entity.setSeatId(booking.getSeatId().value());
        entity.setTotalPrice(booking.getTotalPrice());
        entity.setCurrency(booking.getCurrency());
        entity.setStatus(booking.getStatus());
        entity.setIdempotencyKey(booking.getIdempotencyKey());
        entity.setCreatedAt(booking.getCreatedAt());
        entity.setUpdatedAt(Instant.now());
        entity.setPaymentStatus(DEFAULT_PAYMENT_STATUS);
        // booking_reference, passenger_name, passenger_email are required NOT NULL in DB.
        // These are populated with defaults until the domain model includes passenger details.
        entity.setBookingReference(
                booking.getIdempotencyKey() != null
                        ? booking.getIdempotencyKey()
                        : booking.getId().toString());
        entity.setPassengerName(DEFAULT_PASSENGER_NAME);
        entity.setPassengerEmail(DEFAULT_PASSENGER_EMAIL);
        return entity;
    }
}
