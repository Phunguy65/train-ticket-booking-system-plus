package io.github.phunguy65.ttbs.backend.booking.application.usecase;

import io.github.phunguy65.ttbs.backend.booking.application.command.CancelBookingCommand;
import io.github.phunguy65.ttbs.backend.booking.application.dto.HoldDto;
import io.github.phunguy65.ttbs.backend.booking.domain.errors.BookingError;
import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.port.RouteSeatAvailabilityPort;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import java.util.List;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CancelBookingUseCase {

    private final BookingRepository bookingRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final RouteSeatAvailabilityPort seatAvailabilityPort;

    public CancelBookingUseCase(
            BookingRepository bookingRepository,
            ApplicationEventPublisher eventPublisher,
            RouteSeatAvailabilityPort seatAvailabilityPort) {
        this.bookingRepository = bookingRepository;
        this.eventPublisher = eventPublisher;
        this.seatAvailabilityPort = seatAvailabilityPort;
    }

    @Transactional
    public Result<HoldDto, BookingError> execute(CancelBookingCommand command) {
        Optional<Booking> found =
                bookingRepository.findByIdWithSeats(BookingId.of(command.bookingId()));
        if (found.isEmpty()) {
            return Result.failure(new BookingError.InvalidStatusTransition(null));
        }

        Booking booking = found.get();

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return Result.success(toHoldDto(booking));
        }

        List<SeatId> seatIds =
                booking.getBookedSeats().stream().map(bs -> bs.seatId()).toList();

        if (booking.getStatus() == BookingStatus.HELD) {
            seatAvailabilityPort.releaseHeldSeats(booking.getRouteId(), seatIds);
        } else if (booking.getStatus() == BookingStatus.CONFIRMED) {
            seatAvailabilityPort.cancelBookedSeats(booking.getRouteId(), seatIds);
        }

        Result<Void, BookingError> cancelResult = booking.cancel();
        if (cancelResult.isFailure()) {
            return Result.failure(((Result.Failure<Void, BookingError>) cancelResult).error());
        }

        Booking saved = bookingRepository.save(booking);

        for (DomainEvent event : booking.getDomainEvents()) {
            eventPublisher.publishEvent(event);
        }
        booking.clearDomainEvents();

        return Result.success(toHoldDto(saved));
    }

    private HoldDto toHoldDto(Booking booking) {
        List<HoldDto.BookedSeatDto> seatDtos = booking.getBookedSeats().stream()
                .map(bs -> new HoldDto.BookedSeatDto(bs.seatId().value(), bs.unitPrice()))
                .toList();
        return new HoldDto(
                booking.getId().value(),
                booking.getStatus().name(),
                booking.getRouteId().value(),
                seatDtos,
                booking.getTotalPrice(),
                booking.getCurrency(),
                null,
                null,
                booking.getCheckoutSessionId());
    }
}
