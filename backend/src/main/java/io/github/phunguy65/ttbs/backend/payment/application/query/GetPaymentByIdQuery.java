package io.github.phunguy65.ttbs.backend.payment.application.query;

import java.util.UUID;

public record GetPaymentByIdQuery(UUID paymentId, UUID requestingUserId) {}
