package io.github.phunguy65.ttbs.backend.payment.infrastructure.web;

import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record PaymentHttpResponse(
        UUID paymentId,
        UUID bookingId,
        PaymentStatus status,
        String checkoutUrl,
        BigDecimal amount,
        String currency) {}
