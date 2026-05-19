package io.github.phunguy65.ttbs.backend.booking.domain.projection;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BookingSummary(
        UUID id,
        UUID userId,
        UUID scheduledTripId,
        BookingUserInfoSummary bookerInfo,
        List<BookingPassengerSummary> passengers,
        long totalPrice,
        String currency,
        String status,
        Instant paymentDeadline,
        Instant createdAt) {

    public BookingSummary {
        passengers = passengers == null ? List.of() : List.copyOf(passengers);
    }
}
