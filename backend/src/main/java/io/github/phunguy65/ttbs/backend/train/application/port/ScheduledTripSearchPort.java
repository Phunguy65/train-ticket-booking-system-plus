package io.github.phunguy65.ttbs.backend.train.application.port;

import io.github.phunguy65.ttbs.backend.shared.domain.SliceResponse;
import io.github.phunguy65.ttbs.backend.train.application.query.SearchScheduledTripsCursor;
import io.github.phunguy65.ttbs.backend.train.application.query.SearchScheduledTripsQuery;
import io.github.phunguy65.ttbs.backend.train.domain.projection.ScheduledTripEnrichedSummary;

public interface ScheduledTripSearchPort {

    SliceResponse<ScheduledTripEnrichedSummary> search(
            SearchScheduledTripsQuery query, SearchScheduledTripsCursor cursor);
}
