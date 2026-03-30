package io.github.phunguy65.ttbs.backend.train.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.request.PagedRequest;
import io.github.phunguy65.ttbs.backend.train.application.query.GetCoachesQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;

public record GetCoachesRequest(
        @Min(0) int page, @Min(1) @Max(100) int size) implements PagedRequest {

    public GetCoachesRequest() {
        this(0, 20);
    }

    public GetCoachesQuery toQuery(UUID trainId) {
        return new GetCoachesQuery(page, size, trainId);
    }
}
