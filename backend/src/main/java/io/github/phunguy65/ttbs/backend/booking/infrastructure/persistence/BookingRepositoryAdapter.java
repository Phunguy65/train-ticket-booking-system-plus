package io.github.phunguy65.ttbs.backend.booking.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
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
        return jpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<Booking> findByIdempotencyKey(String idempotencyKey) {
        return jpaRepository.findByIdempotencyKey(idempotencyKey).map(mapper::toDomain);
    }
}
