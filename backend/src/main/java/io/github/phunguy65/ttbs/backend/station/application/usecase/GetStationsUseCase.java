package io.github.phunguy65.ttbs.backend.station.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.station.application.query.GetStationsQuery;
import io.github.phunguy65.ttbs.backend.station.application.response.StationResponse;
import io.github.phunguy65.ttbs.backend.station.domain.projection.StationSummary;
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
        PageResponse<StationSummary> stations =
                stationRepository.findAllSummaries(query.page(), query.size(), sort);
        return PageResponse.of(
                stations.content().stream().map(this::toDto).toList(),
                stations.page(),
                stations.size(),
                stations.hasNext(),
                stations.total());
    }

    private StationResponse toDto(StationSummary station) {
        return new StationResponse(
                station.id(), station.code(), station.name(), station.city(), station.createdAt());
    }
}
