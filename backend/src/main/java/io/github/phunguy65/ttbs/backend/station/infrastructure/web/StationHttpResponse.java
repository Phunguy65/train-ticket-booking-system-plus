package io.github.phunguy65.ttbs.backend.station.infrastructure.web;

import java.time.Instant;
import java.util.UUID;

record StationHttpResponse(UUID id, String code, String name, String city, Instant createdAt) {}
