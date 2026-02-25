package io.github.phunguy65.ttbs.backend.train.domain.repository;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResult;
import io.github.phunguy65.ttbs.backend.shared.domain.SortDirection;
import io.github.phunguy65.ttbs.backend.train.domain.model.Route;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteFilter;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import java.util.Optional;

public interface RouteRepository {

    Route save(Route route);

    Optional<Route> findById(RouteId id);

    PageResult<Route> findAll(
            int page, int size, String sortField, SortDirection direction, RouteFilter filter);
}
