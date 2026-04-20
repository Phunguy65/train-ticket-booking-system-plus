package io.github.phunguy65.ttbs.backend.payment.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.payment.application.query.GetUserPaymentsQuery;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.request.PagedRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;

@Schema(description = "Offset-based pagination query for a customer's payment history.")
public record GetUserPaymentsRequest(
        @Schema(description = "Zero-based page index.", minimum = "0", example = "0") @Min(0) int page,

        @Schema(
                description = "Number of payments per page.",
                minimum = "1",
                maximum = "100",
                example = "20")
        @Min(1) @Max(100) int size)
        implements PagedRequest {

    public GetUserPaymentsRequest() {
        this(0, 20);
    }

    public GetUserPaymentsQuery toQuery(UUID userId, UUID requestingUserId) {
        return new GetUserPaymentsQuery(userId, requestingUserId, page, size);
    }
}
