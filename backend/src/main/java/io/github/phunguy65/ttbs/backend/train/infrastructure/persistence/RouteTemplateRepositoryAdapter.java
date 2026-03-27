package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteTemplate;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteTemplateId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteTemplateRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
class RouteTemplateRepositoryAdapter implements RouteTemplateRepository {

    private final RouteTemplateJpaRepository jpaRepository;
    private final RouteTemplateEntityMapper mapper;

    RouteTemplateRepositoryAdapter(
            RouteTemplateJpaRepository jpaRepository, RouteTemplateEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public RouteTemplate save(RouteTemplate routeTemplate) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(routeTemplate)));
    }

    @Override
    public java.util.Optional<RouteTemplate> findById(RouteTemplateId id) {
        return jpaRepository.findActiveById(id.value()).map(mapper::toDomain);
    }

    @Override
    public PageResponse<RouteTemplate> findAll(int page, int size, List<SortOrder> sort) {
        PageRequest pageable = PageRequest.of(page, size, toSpringSort(sort));
        Page<RouteTemplateEntity> result = jpaRepository.findAllActive(pageable);
        List<RouteTemplate> items =
                result.getContent().stream().map(mapper::toDomain).toList();
        return PageResponse.of(items, page, size, result.hasNext(), result.getTotalElements());
    }

    @Override
    public boolean existsById(RouteTemplateId id) {
        return jpaRepository.existsActiveById(id.value());
    }

    @Override
    public void softDeleteById(RouteTemplateId id, Instant deletedAt) {
        jpaRepository.softDeleteById(id.value(), deletedAt);
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
