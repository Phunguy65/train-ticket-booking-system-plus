package io.github.phunguy65.ttbs.backend.booking.infrastructure.web;

import jakarta.validation.constraints.NotBlank;

/**
 * HTTP request body for {@code POST /api/{version}/bookings/{id}/confirm}.
 *
 * @param paymentReference the payment reference from the payment provider
 */
public record ConfirmSeatHoldHttpRequest(@NotBlank String paymentReference) {}
