package io.github.phunguy65.ttbs.backend.payment.domain.projection;

import java.time.Instant;
import java.util.UUID;

/**
 * Payment summary with nested booking route information for payment history listings.
 *
 * <p>Used by the payment repository to project user payment history data including
 * route information (origin, destination, departure time) from related booking/trip entities.
 */
public record UserPaymentSummary(
        UUID id,
        UUID bookingId,
        UUID userId,
        String status,
        long amount,
        String currency,
        Instant createdAt,
        // Nested booking/route info
        String originStationName,
        String destinationStationName,
        Instant departureTime) {}
