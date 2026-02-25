package io.github.phunguy65.ttbs.backend.station.application.dto;

import java.time.Instant;
import java.util.UUID;

public record StationDto(UUID id, String code, String name, String city, Instant createdAt) {}
