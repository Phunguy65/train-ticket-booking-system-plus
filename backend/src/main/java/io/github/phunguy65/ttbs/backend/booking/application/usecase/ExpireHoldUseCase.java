package io.github.phunguy65.ttbs.backend.booking.application.usecase;

import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.train.application.port.RouteSeatAvailabilityPort;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpireHoldUseCase {

    private static final Logger log = LoggerFactory.getLogger(ExpireHoldUseCase.class);

    private final BookingRepository bookingRepository;
    private final RouteSeatAvailabilityPort seatAvailabilityPort;
    private final ApplicationEventPublisher eventPublisher;

    public ExpireHoldUseCase(
            BookingRepository bookingRepository,
            RouteSeatAvailabilityPort seatAvailabilityPort,
            ApplicationEventPublisher eventPublisher) {
        this.bookingRepository = bookingRepository;
        this.seatAvailabilityPort = seatAvailabilityPort;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void execute(Booking booking) {
        if (booking.getStatus() != BookingStatus.HELD) {
            log.debug(
                    "Booking {} is no longer HELD (status={}), skipping",
                    booking.getId(),
                    booking.getStatus());
            return;
        }

        List<SeatId> seatIds =
                booking.getBookedSeats().stream().map(bs -> bs.seatId()).toList();
        seatAvailabilityPort.releaseHeldSeats(booking.getRouteId(), seatIds);

        booking.expire();
        bookingRepository.save(booking);

        for (DomainEvent event : booking.getDomainEvents()) {
            eventPublisher.publishEvent(event);
        }
        booking.clearDomainEvents();

        log.debug("Expired hold for booking {}", booking.getId());
    }
}
