package io.github.phunguy65.ttbs.backend.booking.application.usecase;

import io.github.phunguy65.ttbs.backend.booking.application.command.ConfirmSeatHoldCommand;
import io.github.phunguy65.ttbs.backend.booking.application.dto.HoldDto;
import io.github.phunguy65.ttbs.backend.booking.domain.errors.BookingError;
import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
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
public class ConfirmSeatHoldUseCase {

    private final BookingRepository bookingRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final RouteSeatAvailabilityPort seatAvailabilityPort;

    public ConfirmSeatHoldUseCase(
            BookingRepository bookingRepository,
            ApplicationEventPublisher eventPublisher,
            RouteSeatAvailabilityPort seatAvailabilityPort) {
        this.bookingRepository = bookingRepository;
        this.eventPublisher = eventPublisher;
        this.seatAvailabilityPort = seatAvailabilityPort;
    }

    @Transactional
    public Result<HoldDto, BookingError> execute(ConfirmSeatHoldCommand command) {
        Optional<Booking> found =
                bookingRepository.findByIdWithSeats(BookingId.of(command.bookingId()));
        if (found.isEmpty()) {
            return Result.failure(new BookingError.InvalidStatusTransition(null));
        }

        Booking booking = found.get();

        Result<Void, BookingError> confirmResult = booking.confirm(command.paymentReference());
        if (confirmResult.isFailure()) {
            BookingError error = ((Result.Failure<Void, BookingError>) confirmResult).error();

            if (error instanceof BookingError.HoldExpired) {
                triggerExpiry(booking);
            }

            return Result.failure(error);
        }

        List<SeatId> seatIds =
                booking.getBookedSeats().stream().map(bs -> bs.seatId()).toList();
        seatAvailabilityPort.confirmHeldSeats(booking.getRouteId(), seatIds);

        Booking saved = bookingRepository.save(booking);

        for (DomainEvent event : booking.getDomainEvents()) {
            eventPublisher.publishEvent(event);
        }
        booking.clearDomainEvents();

        return Result.success(toHoldDto(saved));
    }

    private void triggerExpiry(Booking booking) {
        List<SeatId> seatIds =
                booking.getBookedSeats().stream().map(bs -> bs.seatId()).toList();
        seatAvailabilityPort.releaseHeldSeats(booking.getRouteId(), seatIds);
        booking.expire();
        bookingRepository.save(booking);
        for (DomainEvent event : booking.getDomainEvents()) {
            eventPublisher.publishEvent(event);
        }
        booking.clearDomainEvents();
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
                booking.getPaymentDeadline());
    }
}
