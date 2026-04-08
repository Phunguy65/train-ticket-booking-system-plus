package io.github.phunguy65.ttbs.backend.payment.application.response;

import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Checkout session resource returned after creating or retrieving a payment.")
public record CheckoutSessionResponse(
        @Schema(description = "Payment identifier.", format = "uuid")
        UUID paymentId,

        @Schema(
                description = "Hosted checkout URL where the customer completes the payment.",
                format = "uri")
        String checkoutUrl,

        @Schema(description = "Payment lifecycle status.") PaymentStatus status) {}
