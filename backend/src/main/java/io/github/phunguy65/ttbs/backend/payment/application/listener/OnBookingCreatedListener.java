package io.github.phunguy65.ttbs.backend.payment.application.listener;

import io.github.phunguy65.ttbs.backend.booking.domain.event.BookingCreated;
import io.github.phunguy65.ttbs.backend.payment.application.usecase.CreateCheckoutSessionUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

@Service
public class OnBookingCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(OnBookingCreatedListener.class);

    private final CreateCheckoutSessionUseCase createCheckoutSessionUseCase;

    public OnBookingCreatedListener(CreateCheckoutSessionUseCase createCheckoutSessionUseCase) {
        this.createCheckoutSessionUseCase = createCheckoutSessionUseCase;
    }

    @ApplicationModuleListener
    public void onBookingCreated(BookingCreated event) {
        try {
            createCheckoutSessionUseCase.execute(
                    event.bookingId(), event.userId(), event.totalPrice(), event.currency());
        } catch (Exception e) {
            log.error(
                    "Failed to create checkout session for bookingId={}: {}",
                    event.bookingId(),
                    e.getMessage(),
                    e);
            throw e;
        }
    }
}
