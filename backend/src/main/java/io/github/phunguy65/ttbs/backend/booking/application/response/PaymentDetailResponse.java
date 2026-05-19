package io.github.phunguy65.ttbs.backend.booking.application.response;

import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Payment snapshot associated with a booking.")
public record PaymentDetailResponse(
        @Schema(
                description = "Payment identifier.",
                format = "uuid",
                accessMode = Schema.AccessMode.READ_ONLY)
        UUID id,

        @Schema(description = "Payment lifecycle status.") PaymentStatus status,

        @Schema(
                description = "Hosted checkout URL when further payment action is required.",
                format = "uri",
                nullable = true,
                types = {"string", "null"})
        String checkoutUrl,

        @Schema(description = "Payment amount in minor currency units.", example = "650000")
        long amount,

        @Schema(description = "ISO-like currency code.", example = "VND")
        String currency,

        @Schema(
                description = "Stripe payment intent identifier used for reconciliation.",
                nullable = true,
                types = {"string", "null"})
        String stripePaymentIntentId,

        @Schema(
                description = "Payment creation timestamp.",
                format = "date-time",
                accessMode = Schema.AccessMode.READ_ONLY)
        Instant createdAt) {}
