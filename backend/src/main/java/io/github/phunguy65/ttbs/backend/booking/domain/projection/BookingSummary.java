package io.github.phunguy65.ttbs.backend.booking.domain.projection;

import java.time.Instant;
import java.util.UUID;

public record BookingSummary(
        UUID id,
        UUID userId,
        UUID scheduledTripId,
        BookingUserInfoSummary userInfo,
        long totalPrice,
        String currency,
        String status,
        Instant paymentDeadline,
        Instant createdAt) {}
