package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteSeatAvailability;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteSeatAvailabilityRepository;
import java.util.List;
import java.util.Optional;
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
}
