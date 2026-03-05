package io.github.phunguy65.ttbs.backend.payment.infrastructure.job;

import io.github.phunguy65.ttbs.backend.booking.application.port.StaleHoldQueryPort;
import io.github.phunguy65.ttbs.backend.payment.application.port.PaymentCheckoutSessionPort;
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

    private final StaleHoldQueryPort staleHoldQueryPort;
    private final PaymentCheckoutSessionPort checkoutSessionPort;
    private final ConfirmBookingOnPaymentUseCase confirmBookingOnPaymentUseCase;
    private final CancelBookingOnExpiryUseCase cancelBookingOnExpiryUseCase;

    public PaymentReconciliationJob(
            StaleHoldQueryPort staleHoldQueryPort,
            PaymentCheckoutSessionPort checkoutSessionPort,
            ConfirmBookingOnPaymentUseCase confirmBookingOnPaymentUseCase,
            CancelBookingOnExpiryUseCase cancelBookingOnExpiryUseCase) {
        this.staleHoldQueryPort = staleHoldQueryPort;
        this.checkoutSessionPort = checkoutSessionPort;
        this.confirmBookingOnPaymentUseCase = confirmBookingOnPaymentUseCase;
        this.cancelBookingOnExpiryUseCase = cancelBookingOnExpiryUseCase;
    }

    @Scheduled(fixedDelay = 300_000)
    public void reconcileStaleHolds() {
        Instant threshold = Instant.now().minus(STALE_THRESHOLD_MINUTES, ChronoUnit.MINUTES);
        List<StaleHoldQueryPort.StaleHoldView> staleHolds =
                staleHoldQueryPort.findStaleHoldsWithCheckoutSession(threshold);

        if (staleHolds.isEmpty()) {
            return;
        }

        log.info("Reconciliation: found {} stale HELD bookings to check", staleHolds.size());

        for (StaleHoldQueryPort.StaleHoldView hold : staleHolds) {
            try {
                reconcile(hold);
            } catch (Exception ex) {
                log.error(
                        "Reconciliation failed for bookingId={}: {}",
                        hold.bookingId(),
                        ex.getMessage(),
                        ex);
            }
        }
    }

    private void reconcile(StaleHoldQueryPort.StaleHoldView hold) {
        String sessionId = hold.checkoutSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }

        CheckoutSessionStatus status =
                checkoutSessionPort.retrieveSession(CheckoutSessionId.of(sessionId));

        switch (status) {
            case COMPLETE -> {
                log.info(
                        "Reconciliation: confirming bookingId={} (session={})",
                        hold.bookingId(),
                        sessionId);
                confirmBookingOnPaymentUseCase.execute(hold.bookingId(), null);
            }
            case EXPIRED -> {
                log.info(
                        "Reconciliation: cancelling bookingId={} (session={})",
                        hold.bookingId(),
                        sessionId);
                cancelBookingOnExpiryUseCase.execute(hold.bookingId(), null);
            }
            case OPEN ->
                log.debug(
                        "Reconciliation: session still OPEN for bookingId={}, skipping",
                        hold.bookingId());
        }
    }
}
