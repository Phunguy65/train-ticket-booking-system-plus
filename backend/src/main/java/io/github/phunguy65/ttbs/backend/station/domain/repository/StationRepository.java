package io.github.phunguy65.ttbs.backend.station.domain.repository;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResult;
import io.github.phunguy65.ttbs.backend.shared.domain.SortDirection;
import io.github.phunguy65.ttbs.backend.station.domain.model.Station;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface StationRepository {

    Station save(Station station);

    Optional<Station> findById(StationId id);

    PageResult<Station> findAll(int page, int size, String sortField, SortDirection direction);

    boolean existsByCode(String code);

    void softDeleteById(StationId id, Instant deletedAt);

    int softDeleteByIds(List<StationId> ids, Instant deletedAt);
}
