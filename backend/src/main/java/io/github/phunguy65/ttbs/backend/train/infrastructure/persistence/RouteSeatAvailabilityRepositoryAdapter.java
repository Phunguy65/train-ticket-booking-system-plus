package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteSeatAvailability;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteSeatAvailabilityRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class RouteSeatAvailabilityRepositoryAdapter implements RouteSeatAvailabilityRepository {

    private final RouteSeatAvailabilityJpaRepository jpaRepository;
    private final RouteSeatAvailabilityEntityMapper mapper;

    RouteSeatAvailabilityRepositoryAdapter(
            RouteSeatAvailabilityJpaRepository jpaRepository,
            RouteSeatAvailabilityEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public List<RouteSeatAvailability> findAvailableByRouteId(RouteId routeId) {
        return jpaRepository.findAvailableByRouteId(routeId.value()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<RouteSeatAvailability> findByRouteIdAndSeatId(RouteId routeId, SeatId seatId) {
        return jpaRepository
                .findByRouteIdAndSeatId(routeId.value(), seatId.value())
                .map(mapper::toDomain);
    }

    @Override
    public List<RouteSeatAvailability> findByRouteIdAndSeatIds(
            RouteId routeId, List<SeatId> seatIds) {
        List<UUID> sortedSeatUuids = seatIds.stream()
                .map(SeatId::value)
                .sorted(Comparator.naturalOrder())
                .toList();
        return jpaRepository.findByRouteIdAndSeatIds(routeId.value(), sortedSeatUuids).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<RouteSeatAvailability> findByBookingId(java.util.UUID bookingId) {
        return jpaRepository.findByBookingId(bookingId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<RouteSeatAvailability> saveAll(List<RouteSeatAvailability> records) {
        List<RouteSeatAvailabilityEntity> entities =
                records.stream().map(mapper::toEntity).toList();
        return jpaRepository.saveAll(entities).stream().map(mapper::toDomain).toList();
    }

    @Override
    public RouteSeatAvailability save(RouteSeatAvailability record) {
        RouteSeatAvailabilityEntity entity = mapper.toEntity(record);
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public boolean existsActiveBySeatId(SeatId seatId) {
        return jpaRepository.existsActiveBySeatId(seatId.value());
    }

    @Override
    public boolean existsActiveByAnyOfSeatIds(List<SeatId> seatIds) {
        List<UUID> uuids = seatIds.stream().map(SeatId::value).toList();
        return jpaRepository.existsActiveByAnyOfSeatIds(uuids);
    }

    @Override
    public List<UUID> findActiveBookingIdsBySeatId(SeatId seatId) {
        return jpaRepository.findActiveBookingIdsBySeatId(seatId.value());
    }

    @Override
    public List<UUID> findDistinctActiveBookingIdsBySeatIds(List<SeatId> seatIds) {
        List<UUID> uuids = seatIds.stream().map(SeatId::value).toList();
        return jpaRepository.findDistinctActiveBookingIdsBySeatIds(uuids);
    }

    @Override
    public void hardDeleteByRouteIds(List<RouteId> routeIds) {
        List<UUID> uuids = routeIds.stream().map(RouteId::value).toList();
        jpaRepository.hardDeleteByRouteIds(uuids);
    }

    @Override
    public void hardDeleteBySeatIds(List<SeatId> seatIds) {
        List<UUID> uuids = seatIds.stream().map(SeatId::value).toList();
        jpaRepository.hardDeleteBySeatIds(uuids);
    }
}
