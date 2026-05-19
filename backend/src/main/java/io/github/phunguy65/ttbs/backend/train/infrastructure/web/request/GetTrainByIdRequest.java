package io.github.phunguy65.ttbs.backend.train.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.train.application.query.GetTrainByIdQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "No additional query parameters are accepted when fetching a train.")
public record GetTrainByIdRequest() {

    public GetTrainByIdQuery toQuery(UUID trainId) {
        return new GetTrainByIdQuery(trainId);
    }
}
