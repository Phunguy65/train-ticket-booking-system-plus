package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import io.github.phunguy65.ttbs.backend.train.application.command.CreateRouteCommand;
import io.github.phunguy65.ttbs.backend.train.application.dto.RouteDto;
import io.github.phunguy65.ttbs.backend.train.domain.errors.RouteError;
import io.github.phunguy65.ttbs.backend.train.domain.model.Route;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteRepository;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateRouteUseCase {

    private final RouteRepository routeRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CreateRouteUseCase(
            RouteRepository routeRepository, ApplicationEventPublisher eventPublisher) {
        this.routeRepository = routeRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Result<RouteDto, RouteError> execute(CreateRouteCommand command) {
        RouteId routeId = RouteId.of(UUID.randomUUID());
        Route route = Route.create(
                routeId,
                TrainId.of(command.trainId()),
                StationId.of(command.originStationId()),
                StationId.of(command.destinationStationId()),
                command.departureTime(),
                command.arrivalTime(),
                command.basePrice());

        Route saved = routeRepository.save(route);

        for (DomainEvent event : route.getDomainEvents()) {
            eventPublisher.publishEvent(event);
        }
        route.clearDomainEvents();

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
                route.getBasePrice(),
                route.getStatus(),
                route.getCreatedAt());
    }
}
