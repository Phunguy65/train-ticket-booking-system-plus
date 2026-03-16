package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.Seat;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.SeatRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
class SeatRepositoryAdapter implements SeatRepository {

    private final SeatJpaRepository jpaRepository;
    private final SeatEntityMapper mapper;

    SeatRepositoryAdapter(SeatJpaRepository jpaRepository, SeatEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Seat save(Seat seat) {
        SeatEntity entity = mapper.toEntity(seat);
        SeatEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<Seat> saveAll(List<Seat> seats) {
        List<SeatEntity> entities = seats.stream().map(mapper::toEntity).toList();
        List<SeatEntity> saved = jpaRepository.saveAll(entities);
        return saved.stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Seat> findByCoachId(CoachId coachId) {
        return jpaRepository.findByCoachId(coachId.value()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public PageResponse<Seat> findAll(int page, int size, List<SortOrder> sort, TrainId trainId) {
        PageRequest pageable = PageRequest.of(page, size, toSpringSort(sort));
        Page<SeatEntity> result = jpaRepository.findAllActiveByTrainId(trainId.value(), pageable);
        List<Seat> items = result.getContent().stream().map(mapper::toDomain).toList();
        return PageResponse.of(items, page, size, result.hasNext(), result.getTotalElements());
    }

    @Override
    public PageResponse<Seat> findAllAvailable(
            int page, int size, List<SortOrder> sort, RouteId routeId) {
        PageRequest pageable = PageRequest.of(page, size, toSpringSort(sort));
        Page<SeatEntity> result =
                jpaRepository.findAllAvailableByRouteId(routeId.value(), pageable);
        List<Seat> items = result.getContent().stream().map(mapper::toDomain).toList();
        return PageResponse.of(items, page, size, result.hasNext(), result.getTotalElements());
    }

    @Override
    public Optional<Seat> findById(SeatId id) {
        return jpaRepository.findActiveById(id.value()).map(mapper::toDomain);
    }

    @Override
    public boolean existsByCoachIdAndSeatNumber(CoachId coachId, String seatNumber) {
        return jpaRepository.existsByCoachIdAndSeatNumber(coachId.value(), seatNumber);
    }

    @Override
    public int countActiveByTrainId(TrainId trainId) {
        return jpaRepository.countActiveByTrainId(trainId.value());
    }

    @Override
    public int countActiveByCoachId(CoachId coachId) {
        return jpaRepository.countActiveByCoachId(coachId.value());
    }

    @Override
    public List<TrainId> findDistinctTrainIdsBySeatIds(List<SeatId> seatIds) {
        List<UUID> uuids = seatIds.stream().map(SeatId::value).toList();
        return jpaRepository.findDistinctTrainIdsBySeatIds(uuids).stream()
                .map(TrainId::of)
                .toList();
    }

    @Override
    public List<CoachId> findDistinctCoachIdsBySeatIds(List<SeatId> seatIds) {
        List<UUID> uuids = seatIds.stream().map(SeatId::value).toList();
        return jpaRepository.findDistinctCoachIdsBySeatIds(uuids).stream()
                .map(CoachId::of)
                .toList();
    }

    @Override
    public void softDeleteById(SeatId id, Instant deletedAt) {
        jpaRepository.softDeleteById(id.value(), deletedAt);
    }

    @Override
    public int softDeleteByIds(List<SeatId> ids, Instant deletedAt) {
        List<UUID> uuids = ids.stream().map(SeatId::value).toList();
        return jpaRepository.softDeleteByIds(uuids, deletedAt);
    }

    @Override
    public List<SeatId> findActiveIdsByCoachIds(List<CoachId> coachIds) {
        List<UUID> uuids = coachIds.stream().map(CoachId::value).toList();
        return jpaRepository.findActiveIdsByCoachIds(uuids).stream()
                .map(SeatId::of)
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
