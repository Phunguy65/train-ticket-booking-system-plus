package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.command.UpdateRouteCommand;
import io.github.phunguy65.ttbs.backend.train.application.dto.RouteDto;
import io.github.phunguy65.ttbs.backend.train.domain.error.RouteError;
import io.github.phunguy65.ttbs.backend.train.domain.model.Route;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteStatus;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateRouteUseCase {

    private final RouteRepository routeRepository;

    public UpdateRouteUseCase(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    @Transactional
    public Result<RouteDto, RouteError> execute(UpdateRouteCommand command) {
        Route route = routeRepository.findById(command.routeId()).orElse(null);
        if (route == null) {
            return Result.failure(new RouteError.RouteNotFound());
        }

        Instant newDepartureTime = command.departureTime().isPresent()
                ? command.departureTime().get()
                : route.getDepartureTime();
        Instant newArrivalTime = command.arrivalTime().isPresent()
                ? command.arrivalTime().get()
                : route.getArrivalTime();
        Money newBasePrice =
                command.basePrice().isPresent() ? command.basePrice().get() : route.getBasePrice();
        RouteStatus newStatus =
                command.status().isPresent() ? command.status().get() : route.getStatus();

        Route updated = Route.reconstitute(
                route.getId(),
                route.getTrainId(),
                route.getOriginStationId(),
                route.getDestinationStationId(),
                newDepartureTime,
                newArrivalTime,
                newBasePrice,
                newStatus,
                route.getCreatedAt(),
                route.getDeletedAt());

        Route saved = routeRepository.save(updated);
        return Result.success(toDto(saved));
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
