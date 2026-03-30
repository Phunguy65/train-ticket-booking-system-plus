package io.github.phunguy65.ttbs.backend.payment.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.payment.application.query.GetPaymentByIdQuery;
import java.util.UUID;

public record GetPaymentByIdRequest() {

    public GetPaymentByIdQuery toQuery(UUID paymentId, UUID requestingUserId) {
        return new GetPaymentByIdQuery(paymentId, requestingUserId);
    }
}
