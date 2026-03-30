package io.github.phunguy65.ttbs.backend.payment.application.helper;

import io.github.phunguy65.ttbs.backend.payment.application.response.PaymentResponse;
import io.github.phunguy65.ttbs.backend.payment.domain.error.PaymentError;
import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentStatus;
import io.github.phunguy65.ttbs.backend.payment.domain.projection.PaymentSummary;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PaymentReadAuthorizer {

    public Result<PaymentResponse, PaymentError> authorizeAndMap(
            PaymentSummary payment, UUID requestingUserId) {
        if (payment == null) {
            return Result.failure(new PaymentError.PaymentNotFound());
        }

        if (!payment.userId().equals(requestingUserId)) {
            return Result.failure(new PaymentError.Forbidden());
        }

        return Result.success(new PaymentResponse(
                payment.id(),
                payment.bookingId(),
                PaymentStatus.valueOf(payment.status()),
                payment.checkoutUrl(),
                BigDecimal.valueOf(payment.amount()),
                payment.currency()));
    }
}
