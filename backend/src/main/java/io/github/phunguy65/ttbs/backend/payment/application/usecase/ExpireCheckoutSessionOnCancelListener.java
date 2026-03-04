package io.github.phunguy65.ttbs.backend.payment.application.usecase;

import io.github.phunguy65.ttbs.backend.booking.domain.event.BookingCancelled;
import io.github.phunguy65.ttbs.backend.payment.application.port.CheckoutSessionPort;
import io.github.phunguy65.ttbs.backend.payment.domain.model.CheckoutSessionId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class ExpireCheckoutSessionOnCancelListener {

    private static final Logger log =
            LoggerFactory.getLogger(ExpireCheckoutSessionOnCancelListener.class);

    private final CheckoutSessionPort checkoutSessionPort;

    public ExpireCheckoutSessionOnCancelListener(CheckoutSessionPort checkoutSessionPort) {
        this.checkoutSessionPort = checkoutSessionPort;
    }

    @ApplicationModuleListener
    public void on(BookingCancelled event) {
        String sessionId = event.checkoutSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            log.warn(
                    "BookingCancelled event for bookingId={} has no checkoutSessionId, skipping session expiry",
                    event.bookingId());
            return;
        }
        try {
            checkoutSessionPort.expireSession(CheckoutSessionId.of(sessionId));
        } catch (Exception ex) {
            log.warn(
                    "Failed to expire Stripe session {} for bookingId={}: {}",
                    sessionId,
                    event.bookingId(),
                    ex.getMessage());
        }
    }
}
