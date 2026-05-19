package io.github.phunguy65.ttbs.backend.station.application.port;

import io.github.phunguy65.ttbs.backend.station.application.query.SearchStationsQuery;
import io.github.phunguy65.ttbs.backend.station.domain.projection.StationSummary;
import java.util.List;

public interface StationSearchPort {

    List<StationSummary> search(SearchStationsQuery query);
}
