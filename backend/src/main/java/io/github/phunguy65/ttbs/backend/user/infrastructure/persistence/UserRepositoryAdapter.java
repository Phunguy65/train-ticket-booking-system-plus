package io.github.phunguy65.ttbs.backend.user.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.user.domain.model.User;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import io.github.phunguy65.ttbs.backend.user.domain.projection.UserSummary;
import io.github.phunguy65.ttbs.backend.user.domain.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;
    private final UserEntityMapper mapper;

    UserRepositoryAdapter(UserJpaRepository jpaRepository, UserEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public User save(User user) {
        UserEntity entity = mapper.toEntity(user);
        UserEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findById(UserId id) {
        return jpaRepository.findActiveById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<UserSummary> findSummaryById(UserId id) {
        return jpaRepository.findSummaryById(id.value());
    }

    @Override
    public PageResponse<User> findAll(int page, int size, List<SortOrder> sort) {
        PageRequest pageable = PageRequest.of(page, size, toSpringSort(sort));
        Page<UserEntity> result = jpaRepository.findAllActive(pageable);
        List<User> items = result.getContent().stream().map(mapper::toDomain).toList();
        return PageResponse.of(items, page, size, result.hasNext(), result.getTotalElements());
    }

    @Override
    public PageResponse<UserSummary> findAllSummaries(int page, int size, List<SortOrder> sort) {
        PageRequest pageable = PageRequest.of(page, size, toSpringSort(sort));
        Page<UserSummary> result = jpaRepository.findAllSummaries(pageable);
        return PageResponse.of(
                result.getContent(), page, size, result.hasNext(), result.getTotalElements());
    }

    @Override
    public void softDeleteById(UserId id, Instant deletedAt) {
        jpaRepository.softDeleteById(id.value(), deletedAt);
    }

    @Override
    public int softDeleteByIds(List<UserId> ids, Instant deletedAt) {
        List<UUID> uuids = ids.stream().map(UserId::value).toList();
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
