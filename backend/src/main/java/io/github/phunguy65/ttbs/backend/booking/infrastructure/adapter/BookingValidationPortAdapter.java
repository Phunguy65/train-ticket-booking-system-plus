package io.github.phunguy65.ttbs.backend.booking.infrastructure.adapter;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.booking.infrastructure.persistence.BookingJpaRepository;
import io.github.phunguy65.ttbs.backend.user.application.port.BookingValidationPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adapter that implements {@link BookingValidationPort} (owned by the {@code user} module) using
 * the booking module's JPA repository. Lives in the {@code booking} module's infrastructure layer
 * to avoid coupling the {@code user} module to booking JPA internals.
 */
@Component
class BookingValidationPortAdapter implements BookingValidationPort {

    private final BookingJpaRepository bookingJpaRepository;

    BookingValidationPortAdapter(BookingJpaRepository bookingJpaRepository) {
        this.bookingJpaRepository = bookingJpaRepository;
    }

    @Override
    public boolean hasActiveBookingsForUser(
            io.github.phunguy65.ttbs.backend.user.domain.model.UserId userId) {
        UUID id = userId.value();
        return bookingJpaRepository.existsByUserIdAndStatusIn(
                id, java.util.List.of(BookingStatus.HELD.name(), BookingStatus.CONFIRMED.name()));
    }
}
