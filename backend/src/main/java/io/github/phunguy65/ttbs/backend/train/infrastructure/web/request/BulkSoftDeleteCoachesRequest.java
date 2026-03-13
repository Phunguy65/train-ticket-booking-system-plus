package io.github.phunguy65.ttbs.backend.train.infrastructure.web.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

record BulkSoftDeleteCoachesRequest(
        @NotEmpty @Size(max = 100) List<@NotNull UUID> coachIds) {}
