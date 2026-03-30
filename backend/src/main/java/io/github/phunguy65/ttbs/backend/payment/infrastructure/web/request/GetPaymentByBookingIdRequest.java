package io.github.phunguy65.ttbs.backend.payment.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.payment.application.query.GetPaymentByBookingIdQuery;
import java.util.UUID;

public record GetPaymentByBookingIdRequest() {

    public GetPaymentByBookingIdQuery toQuery(UUID bookingId, UUID requestingUserId) {
        return new GetPaymentByBookingIdQuery(bookingId, requestingUserId);
    }
}
