package io.github.phunguy65.ttbs.backend.payment.application.response;

import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Compact payment summary for payment-history listings.")
public record UserPaymentResponse(
        @Schema(
                description = "Payment identifier.",
                format = "uuid",
                accessMode = Schema.AccessMode.READ_ONLY)
        UUID id,

        @Schema(description = "Payment lifecycle status.") PaymentStatus status,

        @Schema(description = "Payment amount in major currency units.", example = "650000")
        BigDecimal amount,

        @Schema(description = "ISO-like currency code.", example = "VND")
        String currency,

        @Schema(
                description = "Payment creation timestamp.",
                format = "date-time",
                accessMode = Schema.AccessMode.READ_ONLY)
        Instant createdAt,

        @Schema(description = "Booking identifier linked to the payment.", format = "uuid")
        UUID bookingId,

        @Schema(description = "Booking summary with route and travel date information.")
        BookingSummary booking) {

    @Schema(description = "Booking summary embedded in a payment history item.")
    public record BookingSummary(
            @Schema(description = "Booking identifier.", format = "uuid")
            UUID id,

            @Schema(description = "Origin station name.", example = "Hanoi")
            String origin,

            @Schema(description = "Destination station name.", example = "Ho Chi Minh City")
            String destination,

            @Schema(description = "Scheduled departure timestamp.", format = "date-time")
            Instant departureTime) {}
}
