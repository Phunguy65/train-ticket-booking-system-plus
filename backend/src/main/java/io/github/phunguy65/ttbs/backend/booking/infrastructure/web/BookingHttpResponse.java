package io.github.phunguy65.ttbs.backend.booking.infrastructure.web;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import java.time.Instant;
import java.util.UUID;

public record BookingHttpResponse(
        UUID id,
        UUID userId,
        UUID routeId,
        String passengerName,
        String passengerEmail,
        String passengerPhone,
        long totalPrice,
        String currency,
        BookingStatus status,
        Instant paymentDeadline,
        Instant createdAt) {}
