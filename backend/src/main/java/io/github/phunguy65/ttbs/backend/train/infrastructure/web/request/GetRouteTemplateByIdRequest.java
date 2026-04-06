package io.github.phunguy65.ttbs.backend.train.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.train.application.query.GetRouteTemplateByIdQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "No additional query parameters are accepted when fetching a route template.")
public record GetRouteTemplateByIdRequest() {

    public GetRouteTemplateByIdQuery toQuery(UUID routeTemplateId) {
        return new GetRouteTemplateByIdQuery(routeTemplateId);
    }
}
