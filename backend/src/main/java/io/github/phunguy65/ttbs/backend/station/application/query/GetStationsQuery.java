package io.github.phunguy65.ttbs.backend.station.application.query;

import io.github.phunguy65.ttbs.backend.shared.application.query.PagedQuery;

public record GetStationsQuery(int page, int size) implements PagedQuery {}
