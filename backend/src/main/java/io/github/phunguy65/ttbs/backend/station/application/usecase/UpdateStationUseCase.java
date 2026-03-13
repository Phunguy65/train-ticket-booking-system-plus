package io.github.phunguy65.ttbs.backend.station.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.station.application.command.UpdateStationCommand;
import io.github.phunguy65.ttbs.backend.station.application.response.StationResponse;
import io.github.phunguy65.ttbs.backend.station.domain.error.StationError;
import io.github.phunguy65.ttbs.backend.station.domain.model.Station;
import io.github.phunguy65.ttbs.backend.station.domain.repository.StationRepository;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateStationUseCase {

    private final StationRepository stationRepository;

    public UpdateStationUseCase(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
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

        Station updated = Station.reconstitute(
                station.getId(),
                newCode,
                newName,
                newCity,
                station.getCreatedAt(),
                station.getDeletedAt());

        Station saved = stationRepository.save(updated);
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
