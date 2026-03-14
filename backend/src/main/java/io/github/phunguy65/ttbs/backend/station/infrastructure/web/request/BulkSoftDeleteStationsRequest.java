package io.github.phunguy65.ttbs.backend.station.infrastructure.web.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record BulkSoftDeleteStationsRequest(
        @NotEmpty @Size(max = 100) List<@NotNull UUID> stationIds) {}
