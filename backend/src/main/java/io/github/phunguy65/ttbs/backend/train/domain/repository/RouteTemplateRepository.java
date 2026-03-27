package io.github.phunguy65.ttbs.backend.train.domain.repository;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteTemplate;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteTemplateId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RouteTemplateRepository {

    RouteTemplate save(RouteTemplate routeTemplate);

    Optional<RouteTemplate> findById(RouteTemplateId id);

    PageResponse<RouteTemplate> findAll(int page, int size, List<SortOrder> sort);

    boolean existsById(RouteTemplateId id);

    void softDeleteById(RouteTemplateId id, Instant deletedAt);
}
