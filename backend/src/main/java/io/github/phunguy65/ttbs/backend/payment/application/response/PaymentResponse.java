package io.github.phunguy65.ttbs.backend.payment.application.response;

import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record PaymentResponse(
        UUID paymentId,
        UUID bookingId,
        PaymentStatus status,
        String checkoutUrl,
        BigDecimal amount,
        String currency) {}
