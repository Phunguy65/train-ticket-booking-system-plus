package io.github.phunguy65.ttbs.backend.booking.application.usecase;

import io.github.phunguy65.ttbs.backend.booking.application.dto.HoldDto;
import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import java.util.List;
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
    public Optional<HoldDto> execute(UUID id) {
        return bookingRepository.findByIdWithSeats(BookingId.of(id)).map(this::toDto);
    }

    private HoldDto toDto(Booking booking) {
        List<HoldDto.BookedSeatDto> seats = booking.getBookedSeats().stream()
                .map(bs -> new HoldDto.BookedSeatDto(bs.seatId().value(), bs.unitPrice()))
                .toList();
        return new HoldDto(
                booking.getId().value(),
                booking.getStatus().name(),
                booking.getRouteId().value(),
                seats,
                booking.getTotalPrice(),
                booking.getCurrency(),
                booking.getPaymentDeadline());
    }
}
