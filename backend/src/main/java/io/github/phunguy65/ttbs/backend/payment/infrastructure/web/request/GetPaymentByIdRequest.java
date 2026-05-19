package io.github.phunguy65.ttbs.backend.payment.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.payment.application.query.GetPaymentByIdQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "No additional query parameters are accepted when fetching a payment by id.")
public record GetPaymentByIdRequest() {

    public GetPaymentByIdQuery toQuery(UUID paymentId, UUID requestingUserId) {
        return new GetPaymentByIdQuery(paymentId, requestingUserId);
    }
}
