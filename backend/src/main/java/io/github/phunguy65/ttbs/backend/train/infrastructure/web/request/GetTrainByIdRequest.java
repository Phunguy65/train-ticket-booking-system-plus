package io.github.phunguy65.ttbs.backend.train.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.train.application.query.GetTrainByIdQuery;
import java.util.UUID;

public record GetTrainByIdRequest() {

    public GetTrainByIdQuery toQuery(UUID trainId) {
        return new GetTrainByIdQuery(trainId);
    }
}
