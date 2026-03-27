package io.github.phunguy65.ttbs.backend.booking.application.usecase;

import io.github.phunguy65.ttbs.backend.booking.application.command.CancelBookingCommand;
import io.github.phunguy65.ttbs.backend.booking.domain.error.BookingError;
import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.port.RouteSeatAvailabilityManager;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CancelBookingUseCase {

    private final BookingRepository bookingRepository;
    private final RouteSeatAvailabilityManager seatAvailabilityPort;
    private final ApplicationEventPublisher eventPublisher;

    public CancelBookingUseCase(
            BookingRepository bookingRepository,
            RouteSeatAvailabilityManager seatAvailabilityPort,
            ApplicationEventPublisher eventPublisher) {
        this.bookingRepository = bookingRepository;
        this.seatAvailabilityPort = seatAvailabilityPort;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Result<Void, BookingError> execute(CancelBookingCommand command) {
        var found = bookingRepository.findById(BookingId.of(command.bookingId()));
        if (found.isEmpty()) {
            return Result.failure(new BookingError.BookingNotFound());
        }
        Booking booking = found.get();

        if (!booking.getUserId().value().equals(command.requestingUserId())) {
            return Result.failure(new BookingError.Forbidden());
        }

        BookingStatus previousStatus = booking.getStatus();

        Result<Void, BookingError> cancelResult = booking.cancel();
        if (cancelResult.isFailure()) {
            return cancelResult;
        }

        List<SeatId> seatIds = seatAvailabilityPort.findSeatIdsByBookingId(command.bookingId());

        if (!seatIds.isEmpty()) {
            if (previousStatus == BookingStatus.HELD) {
                seatAvailabilityPort.releaseHeldSeats(booking.getScheduledTripId(), seatIds);
            } else if (previousStatus == BookingStatus.CONFIRMED) {
                seatAvailabilityPort.cancelBookedSeats(booking.getScheduledTripId(), seatIds);
            }
        }

        bookingRepository.save(booking);

        for (DomainEvent event : booking.getDomainEvents()) {
            eventPublisher.publishEvent(event);
        }
        booking.clearDomainEvents();

        return Result.success();
    }
}
