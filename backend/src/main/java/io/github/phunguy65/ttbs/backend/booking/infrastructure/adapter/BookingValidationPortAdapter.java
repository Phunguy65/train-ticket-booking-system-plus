package io.github.phunguy65.ttbs.backend.booking.infrastructure.adapter;

import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.user.application.port.BookingValidationPort;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import org.springframework.stereotype.Component;

/**
 * Adapter implementing {@link BookingValidationPort} — delegates to {@link BookingRepository}.
 * Bridges the cross-module boundary while keeping JPA details inside the booking module.
 */
@Component
public class BookingValidationPortAdapter implements BookingValidationPort {

    private final BookingRepository bookingRepository;

    public BookingValidationPortAdapter(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Override
    public boolean hasActiveBookingsForUser(UserId userId) {
        return bookingRepository.existsActiveByUserId(userId);
    }
}
