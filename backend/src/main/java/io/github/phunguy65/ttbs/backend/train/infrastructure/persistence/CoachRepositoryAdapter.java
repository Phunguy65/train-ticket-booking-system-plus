package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.train.domain.model.Coach;
import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.CoachRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
class CoachRepositoryAdapter implements CoachRepository {

    private final CoachJpaRepository jpaRepository;
    private final CoachEntityMapper mapper;

    CoachRepositoryAdapter(CoachJpaRepository jpaRepository, CoachEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Coach save(Coach coach) {
        CoachEntity entity = mapper.toEntity(coach);
        CoachEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<Coach> saveAll(List<Coach> coaches) {
        List<CoachEntity> entities = coaches.stream().map(mapper::toEntity).toList();
        List<CoachEntity> saved = jpaRepository.saveAll(entities);
        return saved.stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Coach> findById(CoachId id) {
        return jpaRepository.findActiveById(id.value()).map(mapper::toDomain);
    }

    @Override
    public List<Coach> findByTrainId(TrainId trainId) {
        return jpaRepository.findAllActiveByTrainId(trainId.value()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public PageResponse<Coach> findAll(int page, int size, List<SortOrder> sort, TrainId trainId) {
        PageRequest pageable = PageRequest.of(page, size, toSpringSort(sort));
        Page<CoachEntity> result = jpaRepository.findAllActiveByTrainId(trainId.value(), pageable);
        List<Coach> items = result.getContent().stream().map(mapper::toDomain).toList();
        return PageResponse.of(items, page, size, result.hasNext(), result.getTotalElements());
    }

    @Override
    public boolean existsByTrainIdAndCarNumber(TrainId trainId, int carNumber) {
        return jpaRepository.existsByTrainIdAndCarNumberAndDeletedAtIsNull(
                trainId.value(), carNumber);
    }

    @Override
    public void softDeleteById(CoachId id, Instant deletedAt) {
        jpaRepository.softDeleteById(id.value(), deletedAt);
    }

    @Override
    public int softDeleteByIds(List<CoachId> ids, Instant deletedAt) {
        List<UUID> uuids = ids.stream().map(CoachId::value).toList();
        return jpaRepository.softDeleteByIds(uuids, deletedAt);
    }

    @Override
    public List<CoachId> findActiveIdsByTrainIds(List<TrainId> trainIds) {
        List<UUID> uuids = trainIds.stream().map(TrainId::value).toList();
        return jpaRepository.findActiveIdsByTrainIds(uuids).stream()
                .map(CoachId::of)
                .toList();
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
