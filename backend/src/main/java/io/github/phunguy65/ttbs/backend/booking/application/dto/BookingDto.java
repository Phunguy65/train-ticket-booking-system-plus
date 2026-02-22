package io.github.phunguy65.ttbs.backend.booking.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BookingDto(
        UUID id,
        UUID userId,
        UUID routeId,
        UUID seatId,
        String status,
        BigDecimal totalPrice,
        String currency,
        String idempotencyKey) {}
