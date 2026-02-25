package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.dto.RouteDto;
import io.github.phunguy65.ttbs.backend.train.domain.errors.RouteError;
import io.github.phunguy65.ttbs.backend.train.domain.model.Route;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetRouteByIdUseCase {

    private final RouteRepository routeRepository;

    public GetRouteByIdUseCase(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    @Transactional(readOnly = true)
    public Result<RouteDto, RouteError> execute(RouteId routeId) {
        return routeRepository
                .findById(routeId)
                .map(route -> Result.<RouteDto, RouteError>success(toDto(route)))
                .orElseGet(() -> Result.failure(new RouteError.RouteNotFound()));
    }

    private RouteDto toDto(Route route) {
        return new RouteDto(
                route.getId().value(),
                route.getTrainId().value(),
                route.getOriginStationId().value(),
                route.getDestinationStationId().value(),
                route.getDepartureTime(),
                route.getArrivalTime(),
                route.getBasePrice(),
                route.getStatus(),
                route.getCreatedAt());
    }
}
