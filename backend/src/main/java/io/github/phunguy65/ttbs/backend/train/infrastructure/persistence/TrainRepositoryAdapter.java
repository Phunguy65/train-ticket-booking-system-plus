package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.train.domain.model.Train;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.projection.TrainSummary;
import io.github.phunguy65.ttbs.backend.train.domain.repository.TrainRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
class TrainRepositoryAdapter implements TrainRepository {

    private final TrainJpaRepository jpaRepository;
    private final TrainEntityMapper mapper;

    TrainRepositoryAdapter(TrainJpaRepository jpaRepository, TrainEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Train save(Train train) {
        TrainEntity entity = mapper.toEntity(train);
        TrainEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public boolean existsById(TrainId id) {
        return jpaRepository.existsById(id.value());
    }

    @Override
    public Optional<Train> findById(TrainId id) {
        return jpaRepository.findActiveById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<TrainSummary> findSummaryById(TrainId id) {
        return jpaRepository.findSummaryById(id.value());
    }

    @Override
    public PageResponse<Train> findAll(int page, int size, List<SortOrder> sort) {
        PageRequest pageable = PageRequest.of(page, size, toSpringSort(sort));
        Page<TrainEntity> result = jpaRepository.findAllActive(pageable);
        List<Train> items = result.getContent().stream().map(mapper::toDomain).toList();
        return PageResponse.of(items, page, size, result.hasNext(), result.getTotalElements());
    }

    @Override
    public PageResponse<TrainSummary> findAllSummaries(int page, int size, List<SortOrder> sort) {
        PageRequest pageable = PageRequest.of(page, size, toSpringSort(sort));
        Page<TrainSummary> result = jpaRepository.findAllSummaries(pageable);
        List<TrainSummary> items = result.getContent();
        return PageResponse.of(items, page, size, result.hasNext(), result.getTotalElements());
    }

    @Override
    public boolean existsByTrainNumber(String trainNumber) {
        return jpaRepository.existsByTrainNumber(trainNumber);
    }

    @Override
    public void softDeleteById(TrainId id, Instant deletedAt) {
        jpaRepository.softDeleteById(id.value(), deletedAt);
    }

    @Override
    public int softDeleteByIds(List<TrainId> ids, Instant deletedAt) {
        List<UUID> uuids = ids.stream().map(TrainId::value).toList();
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
