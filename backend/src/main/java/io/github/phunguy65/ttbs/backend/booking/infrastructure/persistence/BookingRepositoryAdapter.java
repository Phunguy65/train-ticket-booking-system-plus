package io.github.phunguy65.ttbs.backend.booking.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
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
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(booking)));
    }

    @Override
    public Optional<Booking> findById(BookingId bookingId) {
        return jpaRepository.findById(bookingId.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<Booking> findByIdempotencyKey(String idempotencyKey) {
        return jpaRepository.findByIdempotencyKey(idempotencyKey).map(mapper::toDomain);
    }

    @Override
    public Optional<Booking> findActiveHoldByUserAndRoute(UserId userId, RouteId routeId) {
        return jpaRepository
                .findByUserIdAndRouteIdAndStatusHeld(userId.value(), routeId.value())
                .map(mapper::toDomain);
    }

    @Override
    public List<Booking> findExpiredHeldBookings(Instant now) {
        return jpaRepository.findByStatusHeldAndPaymentDeadlineBefore(now).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Booking> saveAll(List<Booking> bookings) {
        List<BookingEntity> entities = bookings.stream().map(mapper::toEntity).toList();
        return jpaRepository.saveAll(entities).stream().map(mapper::toDomain).toList();
    }
}
