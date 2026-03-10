package io.github.phunguy65.ttbs.backend.payment.application.listener;

import io.github.phunguy65.ttbs.backend.booking.domain.event.BookingCancelled;
import io.github.phunguy65.ttbs.backend.payment.application.usecase.ExpireCheckoutSessionUseCase;
import io.github.phunguy65.ttbs.backend.payment.application.usecase.RefundPaymentUseCase;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

@Service
public class OnBookingCancelledListener {

    private final RefundPaymentUseCase refundPaymentUseCase;
    private final ExpireCheckoutSessionUseCase expireCheckoutSessionUseCase;

    public OnBookingCancelledListener(
            RefundPaymentUseCase refundPaymentUseCase,
            ExpireCheckoutSessionUseCase expireCheckoutSessionUseCase) {
        this.refundPaymentUseCase = refundPaymentUseCase;
        this.expireCheckoutSessionUseCase = expireCheckoutSessionUseCase;
    }

    @ApplicationModuleListener
    public void onBookingCancelled(BookingCancelled event) {
        if (event.requiresRefund()) {
            refundPaymentUseCase.execute(event.bookingId());
        } else {
            expireCheckoutSessionUseCase.execute(event.bookingId());
        }
    }
}
