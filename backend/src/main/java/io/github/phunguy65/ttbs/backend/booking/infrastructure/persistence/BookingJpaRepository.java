package io.github.phunguy65.ttbs.backend.booking.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface BookingJpaRepository extends JpaRepository<BookingEntity, UUID> {

    Optional<BookingEntity> findByIdempotencyKey(String idempotencyKey);
}
