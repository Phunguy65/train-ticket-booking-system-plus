package io.github.phunguy65.ttbs.backend.station.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResult;
import io.github.phunguy65.ttbs.backend.shared.domain.SortDirection;
import io.github.phunguy65.ttbs.backend.station.domain.model.Station;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import io.github.phunguy65.ttbs.backend.station.domain.repository.StationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
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
    public PageResult<Station> findAll(
            int page, int size, String sortField, SortDirection direction) {
        Sort.Direction sortDir =
                direction == SortDirection.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(page, size, Sort.by(sortDir, sortField));
        Slice<StationEntity> slice = jpaRepository.findAllActive(pageable);
        List<Station> items = slice.getContent().stream().map(mapper::toDomain).toList();
        return PageResult.of(items, page, size, slice.hasNext());
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
}
