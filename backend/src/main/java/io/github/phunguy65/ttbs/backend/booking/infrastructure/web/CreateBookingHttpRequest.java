package io.github.phunguy65.ttbs.backend.booking.infrastructure.web;

import java.util.UUID;

public record CreateBookingHttpRequest(
        UUID userId, UUID routeId, UUID seatId, String idempotencyKey) {}
