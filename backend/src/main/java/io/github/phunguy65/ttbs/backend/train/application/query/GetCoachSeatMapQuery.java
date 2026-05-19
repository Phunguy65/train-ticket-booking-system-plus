package io.github.phunguy65.ttbs.backend.train.application.query;

import io.github.phunguy65.ttbs.backend.shared.application.query.PagedQuery;
import java.util.UUID;

public record GetCoachSeatMapQuery(int page, int size, UUID scheduledTripId)
        implements PagedQuery {}
