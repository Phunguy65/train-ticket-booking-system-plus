package io.github.phunguy65.ttbs.backend.booking.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.booking.application.query.GetBookingDetailQuery;
import java.util.UUID;

public record GetBookingDetailRequest() {

    public GetBookingDetailQuery toQuery(UUID bookingId, UUID requestingUserId) {
        return new GetBookingDetailQuery(bookingId, requestingUserId);
    }
}
