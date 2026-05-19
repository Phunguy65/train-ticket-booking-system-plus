package io.github.phunguy65.ttbs.backend.station.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.station.domain.model.Station;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import io.github.phunguy65.ttbs.backend.station.domain.projection.StationSummary;
import io.github.phunguy65.ttbs.backend.station.domain.repository.StationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
class StationRepositoryAdapter implements StationRepository {

    private final StationJpaRepository jpaRepository;
    private final StationEntityMapper mapper;

    StationRepositoryAdapter(StationJpaRepository jpaRepository, StationEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Station save(Station station) {
        StationEntity entity = mapper.toEntity(station);
        StationEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Station> findById(StationId id) {
        return jpaRepository.findActiveById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<StationSummary> findSummaryById(StationId id) {
        return jpaRepository.findSummaryById(id.value());
    }

    @Override
    public PageResponse<Station> findAll(int page, int size, List<SortOrder> sort) {
        PageRequest pageable = PageRequest.of(page, size, toSpringSort(sort));
        Page<StationEntity> result = jpaRepository.findAllActive(pageable);
        List<Station> items = result.getContent().stream().map(mapper::toDomain).toList();
        return PageResponse.of(items, page, size, result.hasNext(), result.getTotalElements());
    }

    @Override
    public PageResponse<StationSummary> findAllSummaries(int page, int size, List<SortOrder> sort) {
        PageRequest pageable = PageRequest.of(page, size, toSpringSort(sort));
        Page<StationSummary> result = jpaRepository.findAllSummaries(pageable);
        List<StationSummary> items = result.getContent();
        return PageResponse.of(items, page, size, result.hasNext(), result.getTotalElements());
    }

    @Override
    public boolean existsByCode(String code) {
        return jpaRepository.existsByCode(code);
    }

    @Override
    public void softDeleteById(StationId id, Instant deletedAt) {
        jpaRepository.softDeleteById(id.value(), deletedAt);
    }

    @Override
    public int softDeleteByIds(List<StationId> ids, Instant deletedAt) {
        List<UUID> uuids = ids.stream().map(StationId::value).toList();
        return jpaRepository.softDeleteByIds(uuids, deletedAt);
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
