package io.github.phunguy65.ttbs.backend.station.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.application.response.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.station.application.query.GetStationsQuery;
import io.github.phunguy65.ttbs.backend.station.application.response.StationResponse;
import io.github.phunguy65.ttbs.backend.station.domain.model.Station;
import io.github.phunguy65.ttbs.backend.station.domain.repository.StationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetStationsUseCase {

    private final StationRepository stationRepository;

    public GetStationsUseCase(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<StationResponse> execute(GetStationsQuery query) {
        List<SortOrder> sort = List.of(SortOrder.asc("code"), SortOrder.asc("id"));
        PageResponse<Station> stations =
                stationRepository.findAll(query.page(), query.size(), sort);
        return PageResponse.of(
                stations.content().stream().map(this::toDto).toList(),
                stations.page(),
                stations.size(),
                stations.hasNext(),
                stations.total());
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
