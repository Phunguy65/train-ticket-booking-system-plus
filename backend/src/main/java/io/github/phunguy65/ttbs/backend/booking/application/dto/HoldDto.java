package io.github.phunguy65.ttbs.backend.booking.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Output DTO for a seat hold — returned by {@code CreateSeatHoldUseCase}
 * and {@code ConfirmSeatHoldUseCase}.
 *
 * @param bookingId   the booking identifier
 * @param status      the booking status string (e.g. "HELD", "CONFIRMED")
 * @param routeId     the route identifier
 * @param seats       per-seat price breakdown
 * @param totalPrice  sum of all unit prices
 * @param currency    currency code (e.g. "VND")
 * @param expiresAt   payment deadline for HELD bookings; null for CONFIRMED/CANCELLED
 */
public record HoldDto(
        UUID bookingId,
        String status,
        UUID routeId,
        List<BookedSeatDto> seats,
        BigDecimal totalPrice,
        String currency,
        Instant expiresAt) {

    /**
     * Per-seat data in the hold response.
     */
    public record BookedSeatDto(UUID seatId, BigDecimal unitPrice) {}
}
