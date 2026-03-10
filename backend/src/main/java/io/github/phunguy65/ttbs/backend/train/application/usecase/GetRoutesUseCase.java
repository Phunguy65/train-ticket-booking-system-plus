package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResult;
import io.github.phunguy65.ttbs.backend.shared.domain.SortDirection;
import io.github.phunguy65.ttbs.backend.train.application.dto.RouteDto;
import io.github.phunguy65.ttbs.backend.train.domain.model.Route;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteFilter;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetRoutesUseCase {

    private final RouteRepository routeRepository;

    public GetRoutesUseCase(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<RouteDto> execute(
            int page, int size, String sortField, SortDirection direction, RouteFilter filter) {
        PageResult<Route> routes =
                routeRepository.findAll(page, size, sortField, direction, filter);
        return PageResult.of(
                routes.items().stream().map(this::toDto).toList(),
                routes.pageNumber(),
                routes.pageSize(),
                routes.hasNext());
    }

    private RouteDto toDto(Route route) {
        return new RouteDto(
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
