package io.github.phunguy65.ttbs.backend.booking.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.booking.application.query.GetUserBookingsQuery;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.request.PagedRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;

public record GetUserBookingsRequest(
        @Min(0) int page, @Min(1) @Max(100) int size) implements PagedRequest {

    public GetUserBookingsRequest() {
        this(0, 20);
    }

    public GetUserBookingsQuery toQuery(UUID userId, UUID requestingUserId) {
        return new GetUserBookingsQuery(userId, requestingUserId, page, size);
    }
}
