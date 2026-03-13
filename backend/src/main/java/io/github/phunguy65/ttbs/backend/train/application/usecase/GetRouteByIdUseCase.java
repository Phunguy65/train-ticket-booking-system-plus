package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.response.RouteResponse;
import io.github.phunguy65.ttbs.backend.train.domain.error.RouteError;
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
    public Result<RouteResponse, RouteError> execute(RouteId routeId) {
        return routeRepository
                .findById(routeId)
                .map(route -> Result.<RouteResponse, RouteError>success(toDto(route)))
                .orElseGet(() -> Result.failure(new RouteError.RouteNotFound()));
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
