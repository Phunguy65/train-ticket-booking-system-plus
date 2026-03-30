package io.github.phunguy65.ttbs.backend.train.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.train.application.query.GetRouteTemplateByIdQuery;
import java.util.UUID;

public record GetRouteTemplateByIdRequest() {

    public GetRouteTemplateByIdQuery toQuery(UUID routeTemplateId) {
        return new GetRouteTemplateByIdQuery(routeTemplateId);
    }
}
