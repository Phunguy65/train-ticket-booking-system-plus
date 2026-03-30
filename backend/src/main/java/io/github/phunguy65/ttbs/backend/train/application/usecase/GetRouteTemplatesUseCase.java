package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.train.application.query.GetRouteTemplatesQuery;
import io.github.phunguy65.ttbs.backend.train.application.response.RouteTemplateResponse;
import io.github.phunguy65.ttbs.backend.train.domain.projection.RouteTemplateSummary;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteTemplateRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetRouteTemplatesUseCase {

    private final RouteTemplateRepository routeTemplateRepository;

    public GetRouteTemplatesUseCase(RouteTemplateRepository routeTemplateRepository) {
        this.routeTemplateRepository = routeTemplateRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<RouteTemplateResponse> execute(GetRouteTemplatesQuery query) {
        List<SortOrder> sort = List.of(SortOrder.asc("createdAt"), SortOrder.asc("id"));
        PageResponse<RouteTemplateSummary> templates =
                routeTemplateRepository.findAllSummaries(query.page(), query.size(), sort);
        return PageResponse.of(
                templates.content().stream().map(this::toDto).toList(),
                templates.page(),
                templates.size(),
                templates.hasNext(),
                templates.total());
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
