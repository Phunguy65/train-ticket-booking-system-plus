package io.github.phunguy65.ttbs.backend.payment.application.usecase;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.payment.application.query.GetPaymentByBookingIdQuery;
import io.github.phunguy65.ttbs.backend.payment.application.response.PaymentResponse;
import io.github.phunguy65.ttbs.backend.payment.domain.error.PaymentError;
import io.github.phunguy65.ttbs.backend.payment.domain.projection.PaymentSummary;
import io.github.phunguy65.ttbs.backend.payment.domain.repository.PaymentRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetPaymentByBookingIdUseCase {

    private final PaymentRepository paymentRepository;
    private final PaymentReadAuthorizer paymentReadAuthorizer;

    public GetPaymentByBookingIdUseCase(
            PaymentRepository paymentRepository, PaymentReadAuthorizer paymentReadAuthorizer) {
        this.paymentRepository = paymentRepository;
        this.paymentReadAuthorizer = paymentReadAuthorizer;
    }

    @Transactional(readOnly = true)
    public Result<PaymentResponse, PaymentError> execute(GetPaymentByBookingIdQuery query) {
        PaymentSummary payment = paymentRepository
                .findSummaryByBookingId(BookingId.of(query.bookingId()))
                .orElse(null);

        return paymentReadAuthorizer.authorizeAndMap(payment, query.requestingUserId());
    }
}
