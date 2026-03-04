package io.github.phunguy65.ttbs.backend.station.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.station.application.command.SoftDeleteStationCommand;
import io.github.phunguy65.ttbs.backend.station.domain.errors.StationError;
import io.github.phunguy65.ttbs.backend.station.domain.model.Station;
import io.github.phunguy65.ttbs.backend.station.domain.repository.StationRepository;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SoftDeleteStationUseCase {

    private final StationRepository stationRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SoftDeleteStationUseCase(
            StationRepository stationRepository, ApplicationEventPublisher eventPublisher) {
        this.stationRepository = stationRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Result<Void, StationError> execute(SoftDeleteStationCommand command) {
        Optional<Station> found = stationRepository.findById(command.stationId());
        if (found.isEmpty()) {
            return Result.failure(new StationError.StationNotFound());
        }

        Station station = found.get();

        if (station.isDeleted()) {
            return Result.success();
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
