package io.github.phunguy65.ttbs.backend.station.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResult;
import io.github.phunguy65.ttbs.backend.shared.domain.SortDirection;
import io.github.phunguy65.ttbs.backend.station.application.response.StationResponse;
import io.github.phunguy65.ttbs.backend.station.domain.model.Station;
import io.github.phunguy65.ttbs.backend.station.domain.repository.StationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetStationsUseCase {

    private final StationRepository stationRepository;

    public GetStationsUseCase(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<StationResponse> execute(
            int page, int size, String sortField, SortDirection direction) {
        PageResult<Station> stations = stationRepository.findAll(page, size, sortField, direction);
        return PageResult.of(
                stations.items().stream().map(this::toDto).toList(),
                stations.pageNumber(),
                stations.pageSize(),
                stations.hasNext());
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
