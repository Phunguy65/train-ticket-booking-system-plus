package io.github.phunguy65.ttbs.backend.train.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.request.PagedRequest;
import io.github.phunguy65.ttbs.backend.train.application.query.GetSeatsQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;

public record GetSeatsRequest(
        @Min(0) int page, @Min(1) @Max(100) int size) implements PagedRequest {

    public GetSeatsRequest() {
        this(0, 20);
    }

    public GetSeatsQuery toQuery(UUID trainId) {
        return new GetSeatsQuery(page, size, trainId);
    }
}
