package io.github.phunguy65.ttbs.backend.booking.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class BookingRepositoryAdapter implements BookingRepository {

    private final BookingJpaRepository jpaRepository;
    private final BookingEntityMapper mapper;

    BookingRepositoryAdapter(BookingJpaRepository jpaRepository, BookingEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Booking save(Booking booking) {
        BookingEntity entity = mapper.toEntity(booking);
        BookingEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Booking> findById(BookingId id) {
        return jpaRepository.findByIdWithSeats(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<Booking> findByIdempotencyKey(String idempotencyKey) {
        return jpaRepository.findByIdempotencyKey(idempotencyKey).map(mapper::toDomain);
    }

    @Override
    public Optional<Booking> findActiveHoldByUserIdAndRouteId(UserId userId, RouteId routeId) {
        return jpaRepository
                .findByUserIdAndRouteIdAndStatus(
                        userId.value(), routeId.value(), BookingStatus.HELD)
                .map(mapper::toDomain);
    }

    @Override
    public List<Booking> findExpiredHolds(Instant now, int limit) {
        return jpaRepository.findExpiredHolds(now, limit).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Booking> findByIdWithSeats(BookingId id) {
        return jpaRepository.findByIdWithSeats(id.value()).map(mapper::toDomain);
    }
}
