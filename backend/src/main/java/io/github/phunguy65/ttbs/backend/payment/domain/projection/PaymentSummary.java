package io.github.phunguy65.ttbs.backend.payment.domain.projection;

import java.time.Instant;
import java.util.UUID;

public record PaymentSummary(
        UUID id,
        UUID bookingId,
        UUID userId,
        String status,
        String checkoutUrl,
        long amount,
        String currency,
        String stripePaymentIntentId,
        Instant createdAt) {}
