package io.github.phunguy65.ttbs.backend.station.application.usecase;

import io.github.phunguy65.ttbs.backend.station.application.port.StationSearchPort;
import io.github.phunguy65.ttbs.backend.station.application.query.SearchStationsQuery;
import io.github.phunguy65.ttbs.backend.station.application.response.StationSearchResponse;
import io.github.phunguy65.ttbs.backend.station.domain.projection.StationSummary;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchStationsUseCase {

    private final StationSearchPort stationSearchPort;

    public SearchStationsUseCase(StationSearchPort stationSearchPort) {
        this.stationSearchPort = stationSearchPort;
    }

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "stationSearch",
            key = "'station-search:' + #query.cacheKey()",
            sync = true)
    public List<StationSearchResponse> execute(SearchStationsQuery query) {
        return stationSearchPort.search(query).stream().map(this::toDto).toList();
    }

    private StationSearchResponse toDto(StationSummary station) {
        return new StationSearchResponse(
                station.id(), station.code(), station.name(), station.city());
    }
}
