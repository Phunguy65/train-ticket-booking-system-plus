package io.github.phunguy65.ttbs.backend.booking.application.usecase;

import io.github.phunguy65.ttbs.backend.booking.application.command.CancelBookingCommand;
import io.github.phunguy65.ttbs.backend.booking.domain.error.BookingError;
import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.port.RouteSeatAvailabilityPort;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CancelBookingUseCase {

    private final BookingRepository bookingRepository;
    private final RouteSeatAvailabilityPort seatAvailabilityPort;
    private final ApplicationEventPublisher eventPublisher;

    public CancelBookingUseCase(
            BookingRepository bookingRepository,
            RouteSeatAvailabilityPort seatAvailabilityPort,
            ApplicationEventPublisher eventPublisher) {
        this.bookingRepository = bookingRepository;
        this.seatAvailabilityPort = seatAvailabilityPort;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Result<Void, BookingError> execute(CancelBookingCommand command) {
        // 1. Load booking
        var found = bookingRepository.findById(BookingId.of(command.bookingId()));
        if (found.isEmpty()) {
            return Result.failure(new BookingError.BookingNotFound());
        }
        Booking booking = found.get();

        // 2. Ownership check
        if (!booking.getUserId().value().equals(command.requestingUserId())) {
            return Result.failure(new BookingError.Forbidden());
        }

        // 3. Remember previous status for seat release strategy
        BookingStatus previousStatus = booking.getStatus();

        // 4. Cancel the booking (domain method)
        Result<Void, BookingError> cancelResult = booking.cancel();
        if (cancelResult.isFailure()) {
            return cancelResult;
        }

        // 5. Status-aware seat release
        List<SeatId> seatIds = seatAvailabilityPort.findSeatIdsByBookingId(command.bookingId());

        if (!seatIds.isEmpty()) {
            if (previousStatus == BookingStatus.HELD) {
                seatAvailabilityPort.releaseHeldSeats(booking.getRouteId(), seatIds);
            } else if (previousStatus == BookingStatus.CONFIRMED) {
                seatAvailabilityPort.cancelBookedSeats(booking.getRouteId(), seatIds);
            }
        }

        // 6. Persist
        bookingRepository.save(booking);

        // 7. Publish events
        for (DomainEvent event : booking.getDomainEvents()) {
            eventPublisher.publishEvent(event);
        }
        booking.clearDomainEvents();

        return Result.success();
    }
}
