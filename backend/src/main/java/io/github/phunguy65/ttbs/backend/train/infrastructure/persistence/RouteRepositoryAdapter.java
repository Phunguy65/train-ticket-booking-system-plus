package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResult;
import io.github.phunguy65.ttbs.backend.shared.domain.SortDirection;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import io.github.phunguy65.ttbs.backend.train.domain.model.Route;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteFilter;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
class RouteRepositoryAdapter implements RouteRepository {

    private final RouteJpaRepository jpaRepository;
    private final RouteEntityMapper mapper;

    RouteRepositoryAdapter(RouteJpaRepository jpaRepository, RouteEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Route save(Route route) {
        RouteEntity entity = mapper.toEntity(route);
        RouteEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Route> findById(RouteId id) {
        return jpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public PageResult<Route> findAll(
            int page, int size, String sortField, SortDirection direction, RouteFilter filter) {
        Sort.Direction sortDir =
                direction == SortDirection.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(page, size, Sort.by(sortDir, sortField));
        Slice<RouteEntity> slice = jpaRepository.findAllWithFilter(
                filter.originStationId(),
                filter.destinationStationId(),
                filter.departureDateFrom(),
                filter.departureDateTo(),
                pageable);
        List<Route> items = slice.getContent().stream().map(mapper::toDomain).toList();
        return PageResult.of(items, page, size, slice.hasNext());
    }

    @Override
    public boolean existsActiveByTrainId(TrainId trainId) {
        return jpaRepository.existsActiveRouteByTrainId(trainId.value());
    }

    @Override
    public boolean existsActiveByStationId(StationId stationId) {
        return jpaRepository.existsActiveRouteByStationId(stationId.value());
    }
}
