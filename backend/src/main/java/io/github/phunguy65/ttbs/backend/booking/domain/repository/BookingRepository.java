package io.github.phunguy65.ttbs.backend.booking.domain.repository;

import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import java.util.Optional;

public interface BookingRepository {

    Booking save(Booking booking);

    Optional<Booking> findById(BookingId id);

    Optional<Booking> findByIdempotencyKey(String idempotencyKey);
}
