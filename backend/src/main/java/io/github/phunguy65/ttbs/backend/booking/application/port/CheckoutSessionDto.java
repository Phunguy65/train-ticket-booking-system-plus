package io.github.phunguy65.ttbs.backend.booking.application.port;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CheckoutSessionDto(String checkoutSessionId, String checkoutUrl, Instant expiresAt) {

    public record CreateCommand(UUID bookingId, BigDecimal amountVnd, String idempotencyKey) {}
}
