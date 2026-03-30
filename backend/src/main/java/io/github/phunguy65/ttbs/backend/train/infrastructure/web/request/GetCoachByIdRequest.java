package io.github.phunguy65.ttbs.backend.train.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.train.application.query.GetCoachByIdQuery;
import java.util.UUID;

public record GetCoachByIdRequest() {

    public GetCoachByIdQuery toQuery(UUID coachId, UUID trainId) {
        return new GetCoachByIdQuery(coachId, trainId);
    }
}
