package io.github.phunguy65.ttbs.backend.station.domain.repository;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.station.domain.model.Station;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import io.github.phunguy65.ttbs.backend.station.domain.projection.StationSummary;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface StationRepository {

    Station save(Station station);

    Optional<Station> findById(StationId id);

    Optional<StationSummary> findSummaryById(StationId id);

    PageResponse<Station> findAll(int page, int size, List<SortOrder> sort);

    PageResponse<StationSummary> findAllSummaries(int page, int size, List<SortOrder> sort);

    boolean existsByCode(String code);

    void softDeleteById(StationId id, Instant deletedAt);

    int softDeleteByIds(List<StationId> ids, Instant deletedAt);
}
