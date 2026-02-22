package io.github.phunguy65.ttbs.backend.booking.application.dto;

import java.util.UUID;

public record CreateBookingCommand(UUID userId, UUID routeId, UUID seatId, String idempotencyKey) {}
