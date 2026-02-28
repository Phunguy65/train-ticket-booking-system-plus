package io.github.phunguy65.ttbs.backend.station.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.station.application.command.SoftDeleteStationCommand;
import io.github.phunguy65.ttbs.backend.station.application.port.RouteValidationPort;
import io.github.phunguy65.ttbs.backend.station.domain.errors.StationError;
import io.github.phunguy65.ttbs.backend.station.domain.model.Station;
import io.github.phunguy65.ttbs.backend.station.domain.repository.StationRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SoftDeleteStationUseCase {

    private final StationRepository stationRepository;
    private final RouteValidationPort routeValidationPort;
    private final ApplicationEventPublisher eventPublisher;

    public SoftDeleteStationUseCase(
            StationRepository stationRepository,
            RouteValidationPort routeValidationPort,
            ApplicationEventPublisher eventPublisher) {
        this.stationRepository = stationRepository;
        this.routeValidationPort = routeValidationPort;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Result<Void, StationError> execute(SoftDeleteStationCommand command) {
        Optional<Station> found = stationRepository.findById(command.stationId());
        if (found.isEmpty()) {
            return Result.failure(new StationError.StationNotFound());
        }

        Station station = found.get();

        // Idempotent: already deleted → return success immediately
        if (station.isDeleted()) {
            return Result.success();
        }

        // Guard: block if active routes reference this station
        if (routeValidationPort.hasActiveRoutesForStation(command.stationId())) {
            return Result.failure(
                    new StationError.StationInUse(List.of(command.stationId().value())));
        }

        station.softDelete();
        stationRepository.save(station);

        for (DomainEvent event : station.getDomainEvents()) {
            eventPublisher.publishEvent(event);
        }
        station.clearDomainEvents();

        return Result.success();
    }
}
