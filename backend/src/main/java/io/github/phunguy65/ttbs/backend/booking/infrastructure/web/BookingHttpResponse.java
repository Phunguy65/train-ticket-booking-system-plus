package io.github.phunguy65.ttbs.backend.booking.infrastructure.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * HTTP response body for booking endpoints.
 *
 * <p>Returned for GET, hold, confirm, and cancel operations.
 *
 * @param id             the booking identifier
 * @param userId         the user who owns the booking
 * @param routeId        the route for this booking
 * @param status         the booking status (HELD, CONFIRMED, CANCELLED)
 * @param seats          per-seat breakdown with price snapshots
 * @param totalPrice     sum of all unit prices
 * @param currency       currency code (e.g. "VND")
 * @param idempotencyKey the idempotency key used at creation
 * @param expiresAt      payment deadline for HELD bookings; null otherwise
 */
public record BookingHttpResponse(
        UUID id,
        UUID userId,
        UUID routeId,
        String status,
        List<BookedSeatResponse> seats,
        BigDecimal totalPrice,
        String currency,
        String idempotencyKey,
        Instant expiresAt) {

    /**
     * Per-seat data in the booking response.
     *
     * @param seatId    the seat identifier
     * @param unitPrice the price for this specific seat
     */
    public record BookedSeatResponse(UUID seatId, BigDecimal unitPrice) {}
}
