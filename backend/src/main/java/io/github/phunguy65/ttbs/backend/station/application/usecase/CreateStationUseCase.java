package io.github.phunguy65.ttbs.backend.station.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.station.application.command.CreateStationCommand;
import io.github.phunguy65.ttbs.backend.station.application.dto.StationDto;
import io.github.phunguy65.ttbs.backend.station.domain.errors.StationError;
import io.github.phunguy65.ttbs.backend.station.domain.model.Station;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import io.github.phunguy65.ttbs.backend.station.domain.repository.StationRepository;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateStationUseCase {

    private final StationRepository stationRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CreateStationUseCase(
            StationRepository stationRepository, ApplicationEventPublisher eventPublisher) {
        this.stationRepository = stationRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Result<StationDto, StationError> execute(CreateStationCommand command) {
        if (stationRepository.existsByCode(command.code())) {
            return Result.failure(new StationError.StationCodeAlreadyExists(command.code()));
        }

        StationId stationId = StationId.of(UUID.randomUUID());
        Station station = Station.create(stationId, command.code(), command.name(), command.city());
        Station saved = stationRepository.save(station);

        for (DomainEvent event : station.getDomainEvents()) {
            eventPublisher.publishEvent(event);
        }
        station.clearDomainEvents();

        return Result.success(toDto(saved));
    }

    private StationDto toDto(Station station) {
        return new StationDto(
                station.getId().value(),
                station.getCode(),
                station.getName(),
                station.getCity(),
                station.getCreatedAt());
    }
}
