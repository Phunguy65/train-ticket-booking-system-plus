package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteTemplateId;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTrip;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripId;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.projection.ScheduledTripEnrichedSummary;
import io.github.phunguy65.ttbs.backend.train.domain.projection.ScheduledTripSummary;
import io.github.phunguy65.ttbs.backend.train.domain.repository.ScheduledTripRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
class ScheduledTripRepositoryAdapter implements ScheduledTripRepository {

    private final ScheduledTripJpaRepository jpaRepository;
    private final ScheduledTripEntityMapper mapper;

    ScheduledTripRepositoryAdapter(
            ScheduledTripJpaRepository jpaRepository, ScheduledTripEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public ScheduledTrip save(ScheduledTrip scheduledTrip) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(scheduledTrip)));
    }

    @Override
    public java.util.Optional<ScheduledTrip> findById(ScheduledTripId id) {
        return jpaRepository.findActiveById(id.value()).map(mapper::toDomain);
    }

    @Override
    public java.util.Optional<ScheduledTripSummary> findSummaryById(ScheduledTripId id) {
        return jpaRepository.findSummaryById(id.value());
    }

    @Override
    public java.util.Optional<ScheduledTripEnrichedSummary> findEnrichedById(ScheduledTripId id) {
        return jpaRepository.findEnrichedById(id.value()).map(this::toEnrichedSummary);
    }

    @Override
    public java.util.Optional<ScheduledTripEnrichedSummary> findEnrichedByIdIncludingDeleted(
            ScheduledTripId id) {
        return jpaRepository
                .findEnrichedByIdIncludingDeleted(id.value())
                .map(this::toEnrichedSummary);
    }

    @Override
    public PageResponse<ScheduledTrip> findAll(int page, int size, List<SortOrder> sort) {
        PageRequest pageable = PageRequest.of(page, size, toSpringSort(sort));
        Page<ScheduledTripEntity> result = jpaRepository.findAllActive(pageable);
        List<ScheduledTrip> items =
                result.getContent().stream().map(mapper::toDomain).toList();
        return PageResponse.of(items, page, size, result.hasNext(), result.getTotalElements());
    }

    @Override
    public PageResponse<ScheduledTripSummary> findAllSummaries(
            int page, int size, List<SortOrder> sort) {
        PageRequest pageable = PageRequest.of(page, size, toSpringSort(sort));
        Page<ScheduledTripSummary> result = jpaRepository.findAllSummaries(pageable);
        List<ScheduledTripSummary> items = result.getContent();
        return PageResponse.of(items, page, size, result.hasNext(), result.getTotalElements());
    }

    @Override
    public PageResponse<ScheduledTripEnrichedSummary> findAllEnrichedSummaries(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<ScheduledTripEnrichedSummaryView> result =
                jpaRepository.findAllEnrichedSummaries(pageable);
        List<ScheduledTripEnrichedSummary> items =
                result.getContent().stream().map(this::toEnrichedSummary).toList();
        return PageResponse.of(items, page, size, result.hasNext(), result.getTotalElements());
    }

    @Override
    public boolean existsById(ScheduledTripId id) {
        return jpaRepository.existsActiveById(id.value());
    }

    @Override
    public List<ScheduledTripId> findActiveIdsByTrainIds(List<TrainId> trainIds) {
        List<UUID> uuids = trainIds.stream().map(TrainId::value).toList();
        return jpaRepository.findActiveIdsByTrainIds(uuids).stream()
                .map(ScheduledTripId::of)
                .toList();
    }

    @Override
    public List<ScheduledTripId> findActiveIdsByRouteTemplateId(RouteTemplateId routeTemplateId) {
        return jpaRepository.findActiveIdsByRouteTemplateId(routeTemplateId.value()).stream()
                .map(ScheduledTripId::of)
                .toList();
    }

    @Override
    public void softDeleteById(ScheduledTripId id, Instant deletedAt) {
        jpaRepository.softDeleteById(id.value(), deletedAt);
    }

    @Override
    public int softDeleteByIds(List<ScheduledTripId> ids, Instant deletedAt) {
        List<UUID> uuids = ids.stream().map(ScheduledTripId::value).toList();
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

    private ScheduledTripEnrichedSummary toEnrichedSummary(ScheduledTripEnrichedSummaryView view) {
        return new ScheduledTripEnrichedSummary(
                view.getId(),
                view.getRouteTemplateId(),
                view.getTrainId(),
                view.getDepartureTime(),
                view.getArrivalTime(),
                view.getStatus(),
                view.getCreatedAt(),
                view.getDurationMinutes(),
                view.getAvailableSeatCount(),
                view.getTrainNumber(),
                view.getTrainName(),
                view.getTrainTotalSeats(),
                view.getOriginStationId(),
                view.getOriginStationCode(),
                view.getOriginStationName(),
                view.getOriginStationCity(),
                view.getDestinationStationId(),
                view.getDestinationStationCode(),
                view.getDestinationStationName(),
                view.getDestinationStationCity(),
                view.getRouteBasePrice(),
                view.getRouteCurrency());
    }
}
