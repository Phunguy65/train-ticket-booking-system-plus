package io.github.phunguy65.ttbs.backend.booking.application.usecase;

import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.train.application.port.RouteSeatAvailabilityPort;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpireHeldBookingsUseCase {

    private static final Logger log = LoggerFactory.getLogger(ExpireHeldBookingsUseCase.class);

    private final BookingRepository bookingRepository;
    private final RouteSeatAvailabilityPort seatAvailabilityPort;
    private final ApplicationEventPublisher eventPublisher;

    public ExpireHeldBookingsUseCase(
            BookingRepository bookingRepository,
            RouteSeatAvailabilityPort seatAvailabilityPort,
            ApplicationEventPublisher eventPublisher) {
        this.bookingRepository = bookingRepository;
        this.seatAvailabilityPort = seatAvailabilityPort;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void execute() {
        Instant now = Instant.now();
        List<Booking> expired = bookingRepository.findExpiredHeldBookings(now);

        if (expired.isEmpty()) {
            return;
        }

        log.info("Expiring {} held bookings", expired.size());

        List<Booking> toSave = new ArrayList<>();
        for (Booking booking : expired) {
            var cancelResult = booking.cancel();
            if (cancelResult.isFailure()) {
                log.warn(
                        "Could not cancel booking {}: {}",
                        booking.getBookingId(),
                        ((io.github.phunguy65.ttbs.backend.shared.domain.Result.Failure<?, ?>)
                                        cancelResult)
                                .error());
                continue;
            }

            RouteId trainRouteId = booking.getRouteId();
            List<SeatId> seatIds = seatAvailabilityPort.findSeatIdsByBookingId(
                    booking.getBookingId().value());
            if (!seatIds.isEmpty()) {
                seatAvailabilityPort.releaseHeldSeats(trainRouteId, seatIds);
            }

            toSave.add(booking);
        }

        if (!toSave.isEmpty()) {
            bookingRepository.saveAll(toSave);

            for (Booking booking : toSave) {
                for (DomainEvent event : booking.getDomainEvents()) {
                    eventPublisher.publishEvent(event);
                }
                booking.clearDomainEvents();
            }
        }
    }
}
