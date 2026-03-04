package io.github.phunguy65.ttbs.backend.payment.infrastructure.job;

import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.payment.application.port.CheckoutSessionPort;
import io.github.phunguy65.ttbs.backend.payment.application.usecase.CancelBookingOnExpiryUseCase;
import io.github.phunguy65.ttbs.backend.payment.application.usecase.ConfirmBookingOnPaymentUseCase;
import io.github.phunguy65.ttbs.backend.payment.domain.model.CheckoutSessionId;
import io.github.phunguy65.ttbs.backend.payment.domain.model.CheckoutSessionStatus;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PaymentReconciliationJob {

    private static final Logger log = LoggerFactory.getLogger(PaymentReconciliationJob.class);
    private static final int STALE_THRESHOLD_MINUTES = 35;

    private final BookingRepository bookingRepository;
    private final CheckoutSessionPort checkoutSessionPort;
    private final ConfirmBookingOnPaymentUseCase confirmBookingOnPaymentUseCase;
    private final CancelBookingOnExpiryUseCase cancelBookingOnExpiryUseCase;

    public PaymentReconciliationJob(
            BookingRepository bookingRepository,
            CheckoutSessionPort checkoutSessionPort,
            ConfirmBookingOnPaymentUseCase confirmBookingOnPaymentUseCase,
            CancelBookingOnExpiryUseCase cancelBookingOnExpiryUseCase) {
        this.bookingRepository = bookingRepository;
        this.checkoutSessionPort = checkoutSessionPort;
        this.confirmBookingOnPaymentUseCase = confirmBookingOnPaymentUseCase;
        this.cancelBookingOnExpiryUseCase = cancelBookingOnExpiryUseCase;
    }

    @Scheduled(fixedDelay = 300_000)
    public void reconcileStaleHolds() {
        Instant threshold = Instant.now().minus(STALE_THRESHOLD_MINUTES, ChronoUnit.MINUTES);
        List<Booking> staleHolds = bookingRepository.findStaleHoldsWithCheckoutSession(threshold);

        if (staleHolds.isEmpty()) {
            return;
        }

        log.info("Reconciliation: found {} stale HELD bookings to check", staleHolds.size());

        for (Booking booking : staleHolds) {
            try {
                reconcile(booking);
            } catch (Exception ex) {
                log.error(
                        "Reconciliation failed for bookingId={}: {}",
                        booking.getId().value(),
                        ex.getMessage(),
                        ex);
            }
        }
    }

    private void reconcile(Booking booking) {
        String sessionId = booking.getCheckoutSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }

        CheckoutSessionStatus status =
                checkoutSessionPort.retrieveSession(CheckoutSessionId.of(sessionId));

        switch (status) {
            case COMPLETE -> {
                log.info(
                        "Reconciliation: confirming bookingId={} (session={})",
                        booking.getId().value(),
                        sessionId);
                confirmBookingOnPaymentUseCase.execute(booking.getId().value(), null);
            }
            case EXPIRED -> {
                log.info(
                        "Reconciliation: cancelling bookingId={} (session={})",
                        booking.getId().value(),
                        sessionId);
                cancelBookingOnExpiryUseCase.execute(booking.getId().value(), null);
            }
            case OPEN ->
                log.debug(
                        "Reconciliation: session still OPEN for bookingId={}, skipping",
                        booking.getId().value());
        }
    }
}
