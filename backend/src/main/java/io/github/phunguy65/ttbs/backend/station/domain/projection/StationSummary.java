package io.github.phunguy65.ttbs.backend.station.domain.projection;

import java.time.Instant;
import java.util.UUID;

public record StationSummary(UUID id, String code, String name, String city, Instant createdAt) {}
