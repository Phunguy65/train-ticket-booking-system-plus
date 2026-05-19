package io.github.phunguy65.ttbs.backend.payment.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.payment.application.query.GetPaymentByBookingIdQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(
        description =
                "No additional query parameters are accepted when fetching the payment for a booking.")
public record GetPaymentByBookingIdRequest() {

    public GetPaymentByBookingIdQuery toQuery(UUID bookingId, UUID requestingUserId) {
        return new GetPaymentByBookingIdQuery(bookingId, requestingUserId);
    }
}
