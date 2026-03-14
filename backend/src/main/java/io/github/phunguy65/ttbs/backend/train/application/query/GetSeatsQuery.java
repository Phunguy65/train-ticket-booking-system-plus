package io.github.phunguy65.ttbs.backend.train.application.query;

import java.util.UUID;

public record GetSeatsQuery(int page, int size, UUID trainId) {}
