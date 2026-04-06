package io.github.phunguy65.ttbs.backend.train.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.request.PagedRequest;
import io.github.phunguy65.ttbs.backend.train.application.query.GetSeatsQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;

@Schema(description = "Offset-based pagination query for seats belonging to a train.")
public record GetSeatsRequest(
        @Schema(description = "Zero-based page index.", minimum = "0", example = "0") @Min(0) int page,

        @Schema(
                description = "Number of seats per page.",
                minimum = "1",
                maximum = "100",
                example = "20")
        @Min(1) @Max(100) int size)
        implements PagedRequest {

    public GetSeatsRequest() {
        this(0, 20);
    }

    public GetSeatsQuery toQuery(UUID trainId) {
        return new GetSeatsQuery(page, size, trainId);
    }
}
