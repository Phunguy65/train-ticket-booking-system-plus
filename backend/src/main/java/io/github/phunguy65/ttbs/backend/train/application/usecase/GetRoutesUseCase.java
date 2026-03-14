package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.application.response.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.train.application.query.GetRoutesQuery;
import io.github.phunguy65.ttbs.backend.train.application.response.RouteResponse;
import io.github.phunguy65.ttbs.backend.train.domain.model.Route;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetRoutesUseCase {

    private final RouteRepository routeRepository;

    public GetRoutesUseCase(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<RouteResponse> execute(GetRoutesQuery query) {
        List<SortOrder> sort = List.of(SortOrder.asc("departureTime"), SortOrder.asc("id"));
        PageResponse<Route> routes = routeRepository.findAll(query.page(), query.size(), sort);
        return PageResponse.of(
                routes.content().stream().map(this::toDto).toList(),
                routes.page(),
                routes.size(),
                routes.hasNext(),
                routes.total());
    }

    private RouteResponse toDto(Route route) {
        return new RouteResponse(
                route.getId().value(),
                route.getTrainId().value(),
                route.getOriginStationId().value(),
                route.getDestinationStationId().value(),
                route.getDepartureTime(),
                route.getArrivalTime(),
                route.getBasePrice().toLong(),
                route.getStatus(),
                route.getCreatedAt());
    }
}
