package io.github.phunguy65.ttbs.backend.booking.infrastructure.scheduler;

import io.github.phunguy65.ttbs.backend.booking.application.usecase.ExpireHeldBookingsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that expires held bookings whose {@code payment_deadline} has passed.
 * Runs every 60 seconds with a fixed delay between executions.
 */
@Component
public class BookingExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(BookingExpiryScheduler.class);

    private final ExpireHeldBookingsUseCase expireHeldBookingsUseCase;

    public BookingExpiryScheduler(ExpireHeldBookingsUseCase expireHeldBookingsUseCase) {
        this.expireHeldBookingsUseCase = expireHeldBookingsUseCase;
    }

    @Scheduled(fixedDelay = 60_000)
    public void expireHeldBookings() {
        try {
            expireHeldBookingsUseCase.execute();
        } catch (Exception ex) {
            log.error("Error during booking expiry job", ex);
        }
    }
}
