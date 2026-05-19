package io.github.phunguy65.ttbs.backend.payment.application.response;

import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Payment resource exposed to customers.")
public record PaymentResponse(
        @Schema(description = "Payment identifier.", format = "uuid")
        UUID paymentId,

        @Schema(description = "Booking identifier linked to the payment.", format = "uuid")
        UUID bookingId,

        @Schema(description = "Payment lifecycle status.") PaymentStatus status,

        @Schema(
                description = "Hosted checkout URL when customer action is still required.",
                format = "uri",
                nullable = true,
                types = {"string", "null"})
        String checkoutUrl,

        @Schema(description = "Payment amount in major currency units.", example = "650000")
        BigDecimal amount,

        @Schema(description = "ISO-like currency code.", example = "VND")
        String currency) {}
