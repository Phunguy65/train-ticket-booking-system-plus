package io.github.phunguy65.ttbs.backend.station.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.station.application.dto.StationDto;
import io.github.phunguy65.ttbs.backend.station.domain.errors.StationError;
import io.github.phunguy65.ttbs.backend.station.domain.model.Station;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import io.github.phunguy65.ttbs.backend.station.domain.repository.StationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetStationByIdUseCase {

    private final StationRepository stationRepository;

    public GetStationByIdUseCase(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    @Transactional(readOnly = true)
    public Result<StationDto, StationError> execute(StationId stationId) {
        return stationRepository
                .findById(stationId)
                .map(station -> Result.<StationDto, StationError>success(toDto(station)))
                .orElseGet(() -> Result.failure(new StationError.StationNotFound()));
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
