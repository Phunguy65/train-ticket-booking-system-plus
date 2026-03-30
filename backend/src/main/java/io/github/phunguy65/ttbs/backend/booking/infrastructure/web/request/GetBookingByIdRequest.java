package io.github.phunguy65.ttbs.backend.booking.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.booking.application.query.GetBookingByIdQuery;
import java.util.UUID;

public record GetBookingByIdRequest() {

    public GetBookingByIdQuery toQuery(UUID bookingId, UUID requestingUserId) {
        return new GetBookingByIdQuery(bookingId, requestingUserId);
    }
}
