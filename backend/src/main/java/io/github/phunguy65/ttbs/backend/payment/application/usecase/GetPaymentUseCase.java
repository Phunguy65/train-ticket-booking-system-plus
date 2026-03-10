package io.github.phunguy65.ttbs.backend.payment.application.usecase;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.payment.application.dto.PaymentDto;
import io.github.phunguy65.ttbs.backend.payment.domain.error.PaymentError;
import io.github.phunguy65.ttbs.backend.payment.domain.model.Payment;
import io.github.phunguy65.ttbs.backend.payment.domain.repository.PaymentRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetPaymentUseCase {

    private final PaymentRepository paymentRepository;

    public GetPaymentUseCase(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public Result<PaymentDto, PaymentError> execute(BookingId bookingId, UserId requestingUserId) {
        Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);

        if (payment == null) {
            return Result.failure(new PaymentError.PaymentNotFound());
        }

        if (!payment.getUserId().equals(requestingUserId)) {
            return Result.failure(new PaymentError.PaymentNotFound());
        }

        return Result.success(new PaymentDto(
                payment.getPaymentId().value(),
                payment.getBookingId().value(),
                payment.getStatus(),
                payment.getCheckoutUrl(),
                payment.getAmount().getAmount(),
                payment.getAmount().getCurrency().getCurrencyCode()));
    }
}
