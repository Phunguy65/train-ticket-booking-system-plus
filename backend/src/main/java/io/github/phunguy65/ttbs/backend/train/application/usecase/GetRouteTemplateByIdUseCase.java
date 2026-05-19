package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.query.GetRouteTemplateByIdQuery;
import io.github.phunguy65.ttbs.backend.train.application.response.RouteTemplateResponse;
import io.github.phunguy65.ttbs.backend.train.domain.error.RouteTemplateError;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteTemplateId;
import io.github.phunguy65.ttbs.backend.train.domain.projection.RouteTemplateSummary;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetRouteTemplateByIdUseCase {

    private final RouteTemplateRepository routeTemplateRepository;

    public GetRouteTemplateByIdUseCase(RouteTemplateRepository routeTemplateRepository) {
        this.routeTemplateRepository = routeTemplateRepository;
    }

    @Transactional(readOnly = true)
    public Result<RouteTemplateResponse, RouteTemplateError> execute(
            GetRouteTemplateByIdQuery query) {
        return routeTemplateRepository
                .findSummaryById(RouteTemplateId.of(query.routeTemplateId()))
                .map(summary ->
                        Result.<RouteTemplateResponse, RouteTemplateError>success(toDto(summary)))
                .orElseGet(() -> Result.failure(new RouteTemplateError.RouteTemplateNotFound()));
    }

    private RouteTemplateResponse toDto(RouteTemplateSummary summary) {
        return new RouteTemplateResponse(
                summary.id(),
                summary.originStationId(),
                summary.destinationStationId(),
                summary.basePrice(),
                summary.currency(),
                summary.createdAt());
    }
}
