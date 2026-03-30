package io.github.phunguy65.ttbs.backend.train.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.train.application.query.GetScheduledTripByIdQuery;
import java.util.UUID;

public record GetScheduledTripByIdRequest() {

    public GetScheduledTripByIdQuery toQuery(UUID scheduledTripId) {
        return new GetScheduledTripByIdQuery(scheduledTripId);
    }
}
