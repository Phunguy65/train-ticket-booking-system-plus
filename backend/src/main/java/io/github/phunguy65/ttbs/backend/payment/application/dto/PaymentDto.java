package io.github.phunguy65.ttbs.backend.payment.application.dto;

import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record PaymentDto(
        UUID paymentId,
        UUID bookingId,
        PaymentStatus status,
        String checkoutUrl,
        BigDecimal amount,
        String currency) {}
