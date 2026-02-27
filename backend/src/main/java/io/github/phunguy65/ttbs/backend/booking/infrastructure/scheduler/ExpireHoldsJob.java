package io.github.phunguy65.ttbs.backend.booking.infrastructure.scheduler;

import io.github.phunguy65.ttbs.backend.booking.application.usecase.ExpireHoldUseCase;
import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
    private final ExpireHoldUseCase expireHoldUseCase;

    public ExpireHoldsJob(
            BookingRepository bookingRepository, ExpireHoldUseCase expireHoldUseCase) {
        this.bookingRepository = bookingRepository;
        this.expireHoldUseCase = expireHoldUseCase;
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
                expireHoldUseCase.execute(booking);
            } catch (Exception ex) {
                log.error(
                        "Failed to expire hold for booking {}: {}",
                        booking.getId(),
                        ex.getMessage(),
                        ex);
            }
        }
    }
}
