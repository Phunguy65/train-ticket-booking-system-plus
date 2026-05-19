package io.github.phunguy65.ttbs.backend.payment.application.query;

import io.github.phunguy65.ttbs.backend.shared.application.query.PagedQuery;
import java.util.UUID;

public record GetUserPaymentsQuery(UUID userId, UUID requestingUserId, int page, int size)
        implements PagedQuery {}
