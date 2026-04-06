package io.github.phunguy65.ttbs.backend.booking.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.booking.application.query.GetBookingDetailQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "No additional query parameters are accepted when fetching a booking detail.")
public record GetBookingDetailRequest() {

    public GetBookingDetailQuery toQuery(UUID bookingId, UUID requestingUserId) {
        return new GetBookingDetailQuery(bookingId, requestingUserId);
    }
}
