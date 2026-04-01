package io.github.phunguy65.ttbs.backend.train.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.request.PagedRequest;
import io.github.phunguy65.ttbs.backend.train.application.query.GetCoachSeatMapQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;

public record GetCoachSeatMapRequest(
        @Min(0) int page, @Min(1) @Max(100) int size) implements PagedRequest {

    public GetCoachSeatMapRequest() {
        this(0, 20);
    }

    public GetCoachSeatMapQuery toQuery(UUID scheduledTripId) {
        return new GetCoachSeatMapQuery(page, size, scheduledTripId);
    }
}
