package io.github.phunguy65.ttbs.backend.booking.application.response;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import java.time.Instant;
import java.util.UUID;

public record UserBookingResponse(
        UUID id,
        UUID userId,
        UUID scheduledTripId,
        long totalPrice,
        String currency,
        BookingStatus status,
        Instant paymentDeadline,
        Instant createdAt) {}
