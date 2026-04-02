package io.github.phunguy65.ttbs.backend.station.application.response;

import java.util.UUID;

public record StationSearchResponse(UUID id, String code, String name, String city) {}
