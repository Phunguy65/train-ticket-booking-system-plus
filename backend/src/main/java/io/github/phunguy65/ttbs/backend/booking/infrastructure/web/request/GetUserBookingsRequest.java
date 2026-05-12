package io.github.phunguy65.ttbs.backend.booking.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.booking.application.query.GetUserBookingsQuery;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.request.PagedRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;

@Schema(description = "Offset-based pagination query for a customer's booking history.")
public record GetUserBookingsRequest(
        @Schema(description = "Zero-based page index.", minimum = "0", example = "0") @Min(0) Integer page,

        @Schema(
                description = "Number of bookings per page.",
                minimum = "1",
                maximum = "100",
                example = "20")
        @Min(1) @Max(100) Integer size)
        implements PagedRequest {

    public GetUserBookingsRequest {
        if (page == null) {
            page = 0;
        }
        if (size == null) {
            size = 20;
        }
    }

    public GetUserBookingsRequest() {
        this(0, 20);
    }

    public GetUserBookingsQuery toQuery(UUID userId, UUID requestingUserId) {
        return new GetUserBookingsQuery(userId, requestingUserId, page, size);
    }
}
