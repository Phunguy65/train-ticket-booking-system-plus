package io.github.phunguy65.ttbs.backend.train.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.train.application.query.GetScheduledTripByIdQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "No additional query parameters are accepted when fetching a scheduled trip.")
public record GetScheduledTripByIdRequest() {

    public GetScheduledTripByIdQuery toQuery(UUID scheduledTripId) {
        return new GetScheduledTripByIdQuery(scheduledTripId);
    }
}
