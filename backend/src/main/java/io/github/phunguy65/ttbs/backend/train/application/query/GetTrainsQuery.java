package io.github.phunguy65.ttbs.backend.train.application.query;

import io.github.phunguy65.ttbs.backend.shared.application.query.PagedQuery;

public record GetTrainsQuery(int page, int size) implements PagedQuery {}
