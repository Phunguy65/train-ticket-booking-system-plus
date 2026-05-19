package io.github.phunguy65.ttbs.backend.train.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.train.application.query.GetCoachByIdQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "No additional query parameters are accepted when fetching a coach.")
public record GetCoachByIdRequest() {

    public GetCoachByIdQuery toQuery(UUID coachId, UUID trainId) {
        return new GetCoachByIdQuery(coachId, trainId);
    }
}
