package io.github.phunguy65.ttbs.backend.train.domain.repository;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import io.github.phunguy65.ttbs.backend.train.domain.model.Route;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RouteRepository {

    Route save(Route route);

    Optional<Route> findById(RouteId id);

    PageResponse<Route> findAll(int page, int size, List<SortOrder> sort);

    boolean existsActiveByTrainId(TrainId trainId);

    boolean existsActiveByStationId(StationId stationId);

    boolean existsById(RouteId id);

    List<RouteId> findActiveIdsByTrainIds(List<TrainId> trainIds);

    List<RouteId> findActiveIdsByStationId(StationId stationId);

    List<RouteId> findActiveIdsByStationIds(List<StationId> stationIds);

    List<TrainId> findDistinctActiveTrainIdsByRouteIds(List<RouteId> routeIds);

    long countActiveByTrainId(TrainId trainId);

    void softDeleteById(RouteId id, Instant deletedAt);

    int softDeleteByIds(List<RouteId> ids, Instant deletedAt);
}
