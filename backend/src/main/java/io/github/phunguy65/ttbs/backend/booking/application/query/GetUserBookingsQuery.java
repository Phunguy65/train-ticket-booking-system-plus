package io.github.phunguy65.ttbs.backend.booking.application.query;

import io.github.phunguy65.ttbs.backend.shared.application.query.PagedQuery;
import java.util.UUID;

public record GetUserBookingsQuery(UUID userId, UUID requestingUserId, int page, int size)
        implements PagedQuery {}
