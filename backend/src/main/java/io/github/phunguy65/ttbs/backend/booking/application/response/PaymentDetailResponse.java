package io.github.phunguy65.ttbs.backend.booking.application.response;

import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentStatus;
import java.time.Instant;
import java.util.UUID;

public record PaymentDetailResponse(
        UUID id,
        PaymentStatus status,
        String checkoutUrl,
        long amount,
        String currency,
        String stripePaymentIntentId,
        Instant createdAt) {}
