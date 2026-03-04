package io.github.phunguy65.ttbs.backend.booking.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Output DTO for a seat hold — returned by {@code CreateSeatHoldUseCase},
 * {@code ConfirmSeatHoldUseCase}, and {@code CancelBookingUseCase}.
 *
 * @param bookingId          the booking identifier
 * @param status             the booking status string (e.g. "HELD", "CONFIRMED")
 * @param routeId            the route identifier
 * @param seats              per-seat price breakdown
 * @param totalPrice         sum of all unit prices
 * @param currency           currency code (e.g. "VND")
 * @param expiresAt          Stripe session expiry for HELD bookings; null otherwise
 * @param checkoutUrl        Stripe Checkout URL for HELD bookings; null otherwise
 * @param checkoutSessionId  Stripe Checkout Session ID; null for non-HELD bookings
 */
public record HoldDto(
        UUID bookingId,
        String status,
        UUID routeId,
        List<BookedSeatDto> seats,
        BigDecimal totalPrice,
        String currency,
        Instant expiresAt,
        String checkoutUrl,
        String checkoutSessionId) {

    /**
     * Per-seat data in the hold response.
     */
    public record BookedSeatDto(UUID seatId, BigDecimal unitPrice) {}
}
