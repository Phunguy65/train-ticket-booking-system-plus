package io.github.phunguy65.ttbs.backend.user.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.user.application.query.GetUsersQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record GetUsersRequest(
        @Min(0) int page, @Min(1) @Max(100) int size) {

    public GetUsersRequest() {
        this(0, 20);
    }

    public GetUsersQuery toQuery() {
        return new GetUsersQuery(page, size);
    }
}
