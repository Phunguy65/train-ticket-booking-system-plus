package io.github.phunguy65.ttbs.backend.station.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.station.application.query.GetStationByIdQuery;
import io.github.phunguy65.ttbs.backend.station.application.response.StationResponse;
import io.github.phunguy65.ttbs.backend.station.domain.error.StationError;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import io.github.phunguy65.ttbs.backend.station.domain.projection.StationSummary;
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
    public Result<StationResponse, StationError> execute(GetStationByIdQuery query) {
        return stationRepository
                .findSummaryById(StationId.of(query.stationId()))
                .map(summary -> Result.<StationResponse, StationError>success(toDto(summary)))
                .orElseGet(() -> Result.failure(new StationError.StationNotFound()));
    }

    private StationResponse toDto(StationSummary station) {
        return new StationResponse(
                station.id(), station.code(), station.name(), station.city(), station.createdAt());
    }
}
