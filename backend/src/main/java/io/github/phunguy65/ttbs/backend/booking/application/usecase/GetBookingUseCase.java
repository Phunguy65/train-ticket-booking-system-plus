package io.github.phunguy65.ttbs.backend.booking.application.usecase;

import io.github.phunguy65.ttbs.backend.booking.application.dto.BookingDto;
import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetBookingUseCase {

    private final BookingRepository bookingRepository;

    public GetBookingUseCase(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Transactional(readOnly = true)
    public Optional<BookingDto> execute(UUID id) {
        return bookingRepository.findById(BookingId.of(id)).map(this::toDto);
    }

    private BookingDto toDto(Booking booking) {
        return new BookingDto(
                booking.getId().value(),
                booking.getUserId().value(),
                booking.getRouteId().value(),
                booking.getSeatId().value(),
                booking.getStatus().name(),
                booking.getTotalPrice(),
                booking.getCurrency(),
                booking.getIdempotencyKey());
    }
}
