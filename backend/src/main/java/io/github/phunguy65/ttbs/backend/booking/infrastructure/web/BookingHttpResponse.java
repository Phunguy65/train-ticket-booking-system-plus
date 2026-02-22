package io.github.phunguy65.ttbs.backend.booking.infrastructure.web;

import java.math.BigDecimal;
import java.util.UUID;

public record BookingHttpResponse(
        UUID id,
        UUID userId,
        UUID routeId,
        UUID seatId,
        String status,
        BigDecimal totalPrice,
        String currency,
        String idempotencyKey) {}
