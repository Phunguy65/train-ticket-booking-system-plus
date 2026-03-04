package io.github.phunguy65.ttbs.backend.booking.infrastructure.adapter;

import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.train.application.port.validation.SeatValidationPort;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Adapter implementing {@link SeatValidationPort} — delegates to {@link BookingRepository}.
 * Bridges the cross-module boundary while keeping JPA details inside the booking module.
 */
@Component
public class SeatValidationPortAdapter implements SeatValidationPort {

    private final BookingRepository bookingRepository;

    public SeatValidationPortAdapter(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Override
    public boolean hasBookingHistoryForSeat(SeatId seatId) {
        return bookingRepository.existsBySeatId(seatId);
    }

    @Override
    public boolean hasBookingHistoryForAnySeats(List<SeatId> seatIds) {
        return seatIds.stream().anyMatch(this::hasBookingHistoryForSeat);
    }
}
