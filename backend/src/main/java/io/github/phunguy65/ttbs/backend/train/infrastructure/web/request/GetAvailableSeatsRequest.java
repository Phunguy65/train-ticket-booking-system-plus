package io.github.phunguy65.ttbs.backend.train.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.request.PagedRequest;
import io.github.phunguy65.ttbs.backend.train.application.query.GetAvailableSeatsQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;

public record GetAvailableSeatsRequest(
        @Min(0) int page, @Min(1) @Max(100) int size) implements PagedRequest {

    public GetAvailableSeatsRequest() {
        this(0, 20);
    }

    public GetAvailableSeatsQuery toQuery(UUID scheduledTripId) {
        return new GetAvailableSeatsQuery(page, size, scheduledTripId);
    }
}
