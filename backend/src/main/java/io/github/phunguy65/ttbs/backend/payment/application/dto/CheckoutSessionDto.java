package io.github.phunguy65.ttbs.backend.payment.application.dto;

import java.time.Instant;

public record CheckoutSessionDto(String checkoutSessionId, String checkoutUrl, Instant expiresAt) {}
