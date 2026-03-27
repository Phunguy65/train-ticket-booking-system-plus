package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import io.github.phunguy65.ttbs.backend.train.domain.model.Route;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
        return jpaRepository.findActiveById(id.value()).map(mapper::toDomain);
    }

    @Override
    public PageResponse<Route> findAll(int page, int size, List<SortOrder> sort) {
        PageRequest pageable = PageRequest.of(page, size, toSpringSort(sort));
        Page<RouteEntity> result = jpaRepository.findAllActive(pageable);
        List<Route> items = result.getContent().stream().map(mapper::toDomain).toList();
        return PageResponse.of(items, page, size, result.hasNext(), result.getTotalElements());
    }

    @Override
    public boolean existsActiveByTrainId(TrainId trainId) {
        return jpaRepository.existsActiveRouteByTrainId(trainId.value());
    }

    @Override
    public boolean existsActiveByStationId(StationId stationId) {
        return jpaRepository.existsActiveRouteByStationId(stationId.value());
    }

    @Override
    public boolean existsById(RouteId id) {
        return jpaRepository.existsActiveById(id.value());
    }

    @Override
    public List<RouteId> findActiveIdsByTrainIds(List<TrainId> trainIds) {
        List<UUID> uuids = trainIds.stream().map(TrainId::value).toList();
        return jpaRepository.findActiveIdsByTrainIds(uuids).stream()
                .map(RouteId::of)
                .toList();
    }

    @Override
    public void softDeleteById(RouteId id, Instant deletedAt) {
        jpaRepository.softDeleteById(id.value(), deletedAt);
    }

    @Override
    public int softDeleteByIds(List<RouteId> ids, Instant deletedAt) {
        List<UUID> uuids = ids.stream().map(RouteId::value).toList();
        return jpaRepository.softDeleteByIds(uuids, deletedAt);
    }

    @Override
    public List<RouteId> findActiveIdsByStationId(StationId stationId) {
        return jpaRepository.findActiveIdsByStationId(stationId.value()).stream()
                .map(RouteId::of)
                .toList();
    }

    @Override
    public List<RouteId> findActiveIdsByStationIds(List<StationId> stationIds) {
        List<UUID> uuids = stationIds.stream().map(StationId::value).toList();
        return jpaRepository.findActiveIdsByStationIds(uuids).stream()
                .map(RouteId::of)
                .toList();
    }

    @Override
    public List<TrainId> findDistinctActiveTrainIdsByRouteIds(List<RouteId> routeIds) {
        List<UUID> uuids = routeIds.stream().map(RouteId::value).toList();
        return jpaRepository.findDistinctActiveTrainIdsByRouteIds(uuids).stream()
                .map(TrainId::of)
                .toList();
    }

    @Override
    public long countActiveByTrainId(TrainId trainId) {
        return jpaRepository.countActiveByTrainId(trainId.value());
    }

    private Sort toSpringSort(List<SortOrder> orders) {
        List<Sort.Order> springOrders = orders.stream()
                .map(o -> o.direction() == SortOrder.Direction.ASC
                        ? Sort.Order.asc(o.field())
                        : Sort.Order.desc(o.field()))
                .toList();
        return Sort.by(springOrders);
    }
}
