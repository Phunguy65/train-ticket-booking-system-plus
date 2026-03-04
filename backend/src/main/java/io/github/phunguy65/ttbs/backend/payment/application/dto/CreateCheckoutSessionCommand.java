package io.github.phunguy65.ttbs.backend.payment.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateCheckoutSessionCommand(
        UUID bookingId, BigDecimal amountVnd, String idempotencyKey) {}
