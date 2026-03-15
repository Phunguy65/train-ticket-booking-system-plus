package io.github.phunguy65.ttbs.backend.station.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.station.application.command.UpdateStationCommand;
import io.github.phunguy65.ttbs.backend.station.application.response.StationResponse;
import io.github.phunguy65.ttbs.backend.station.domain.error.StationError;
import io.github.phunguy65.ttbs.backend.station.domain.model.Station;
import io.github.phunguy65.ttbs.backend.station.domain.repository.StationRepository;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateStationUseCase {

    private final StationRepository stationRepository;
    private final ApplicationEventPublisher eventPublisher;

    public UpdateStationUseCase(
            StationRepository stationRepository, ApplicationEventPublisher eventPublisher) {
        this.stationRepository = stationRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Result<StationResponse, StationError> execute(UpdateStationCommand command) {
        Station station = stationRepository.findById(command.stationId()).orElse(null);
        if (station == null) {
            return Result.failure(new StationError.StationNotFound());
        }

        JsonNullable<String> codeField = command.code();
        if (codeField.isPresent()) {
            String newCode = codeField.get();
            if (newCode != null && !newCode.equals(station.getCode())) {
                if (stationRepository.existsByCode(newCode)) {
                    return Result.failure(new StationError.StationCodeAlreadyExists(newCode));
                }
            }
        }

        String newCode = codeField.isPresent() ? codeField.get() : station.getCode();
        String newName = command.name().isPresent() ? command.name().get() : station.getName();
        String newCity = command.city().isPresent() ? command.city().get() : station.getCity();

        Station updated = station.update(newCode, newName, newCity);
        Station saved = stationRepository.save(updated);

        for (DomainEvent event : updated.getDomainEvents()) {
            eventPublisher.publishEvent(event);
        }
        updated.clearDomainEvents();

        return Result.success(toDto(saved));
    }

    private StationResponse toDto(Station station) {
        return new StationResponse(
                station.getId().value(),
                station.getCode(),
                station.getName(),
                station.getCity(),
                station.getCreatedAt());
    }
}
