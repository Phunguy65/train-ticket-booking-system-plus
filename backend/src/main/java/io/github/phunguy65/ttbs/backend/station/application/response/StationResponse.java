package io.github.phunguy65.ttbs.backend.station.application.response;

import java.time.Instant;
import java.util.UUID;

public record StationResponse(UUID id, String code, String name, String city, Instant createdAt) {}
