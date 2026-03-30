package io.github.phunguy65.ttbs.backend.payment.application.usecase;

import io.github.phunguy65.ttbs.backend.payment.application.query.GetPaymentByIdQuery;
import io.github.phunguy65.ttbs.backend.payment.application.response.PaymentResponse;
import io.github.phunguy65.ttbs.backend.payment.domain.error.PaymentError;
import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentId;
import io.github.phunguy65.ttbs.backend.payment.domain.projection.PaymentSummary;
import io.github.phunguy65.ttbs.backend.payment.domain.repository.PaymentRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetPaymentByIdUseCase {

    private final PaymentRepository paymentRepository;
    private final PaymentReadAuthorizer paymentReadAuthorizer;

    public GetPaymentByIdUseCase(
            PaymentRepository paymentRepository, PaymentReadAuthorizer paymentReadAuthorizer) {
        this.paymentRepository = paymentRepository;
        this.paymentReadAuthorizer = paymentReadAuthorizer;
    }

    @Transactional(readOnly = true)
    public Result<PaymentResponse, PaymentError> execute(GetPaymentByIdQuery query) {
        PaymentSummary payment = paymentRepository
                .findSummaryById(PaymentId.of(query.paymentId()))
                .orElse(null);

        return paymentReadAuthorizer.authorizeAndMap(payment, query.requestingUserId());
    }
}
