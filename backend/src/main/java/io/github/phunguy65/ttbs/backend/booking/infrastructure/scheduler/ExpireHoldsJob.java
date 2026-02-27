package io.github.phunguy65.ttbs.backend.booking.infrastructure.scheduler;

import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.train.application.port.RouteSeatAvailabilityPort;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled job that polls for expired seat holds and releases their seats.
 *
 * <p>Runs with a fixed delay of 60 seconds after the previous execution completes.
 * Processes expired holds in batches of 100.
 *
 * <p>For each expired hold:
 * <ol>
 *   <li>Verifies the hold is still in HELD status (guards against concurrent confirmation)
 *   <li>Releases all associated seats back to AVAILABLE
 *   <li>Transitions the booking to CANCELLED
 *   <li>Publishes {@code SeatHoldExpired} domain event
 * </ol>
 */
@Component
public class ExpireHoldsJob {

    private static final Logger log = LoggerFactory.getLogger(ExpireHoldsJob.class);
    private static final int BATCH_SIZE = 100;

    private final BookingRepository bookingRepository;
    private final RouteSeatAvailabilityPort seatAvailabilityPort;
    private final ApplicationEventPublisher eventPublisher;

    public ExpireHoldsJob(
            BookingRepository bookingRepository,
            RouteSeatAvailabilityPort seatAvailabilityPort,
            ApplicationEventPublisher eventPublisher) {
        this.bookingRepository = bookingRepository;
        this.seatAvailabilityPort = seatAvailabilityPort;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedDelay = 60_000)
    public void expireStaleHolds() {
        Instant now = Instant.now();
        List<Booking> expiredHolds = bookingRepository.findExpiredHolds(now, BATCH_SIZE);

        if (expiredHolds.isEmpty()) {
            return;
        }

        log.info("Found {} expired holds to process", expiredHolds.size());

        for (Booking booking : expiredHolds) {
            try {
                processExpiredHold(booking);
            } catch (Exception ex) {
                log.error(
                        "Failed to expire hold for booking {}: {}",
                        booking.getId(),
                        ex.getMessage(),
                        ex);
            }
        }
    }

    @Transactional
    public void processExpiredHold(Booking booking) {
        // Re-validate status (guard against concurrent confirmation)
        if (booking.getStatus() != BookingStatus.HELD) {
            log.debug(
                    "Booking {} is no longer HELD (status={}), skipping",
                    booking.getId(),
                    booking.getStatus());
            return;
        }

        // Release all seats back to AVAILABLE
        List<SeatId> seatIds =
                booking.getBookedSeats().stream().map(bs -> bs.seatId()).toList();
        seatAvailabilityPort.releaseHeldSeats(booking.getRouteId(), seatIds);

        // Expire the booking (HELD → CANCELLED)
        booking.expire();
        bookingRepository.save(booking);

        // Publish SeatHoldExpired event
        for (DomainEvent event : booking.getDomainEvents()) {
            eventPublisher.publishEvent(event);
        }
        booking.clearDomainEvents();

        log.debug("Expired hold for booking {}", booking.getId());
    }
}
