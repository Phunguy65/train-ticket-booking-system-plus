package io.github.phunguy65.ttbs.backend.station.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.station.application.command.UpdateStationCommand;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.openapitools.jackson.nullable.JsonNullable;

public record PatchStationRequest(
        @NotBlank @Size(max = 10) JsonNullable<String> code,
        @NotBlank @Size(max = 255) JsonNullable<String> name,
        @NotBlank @Size(max = 100) JsonNullable<String> city) {

    PatchStationRequest() {
        this(JsonNullable.undefined(), JsonNullable.undefined(), JsonNullable.undefined());
    }

    public UpdateStationCommand toCommand(UUID id) {
        return new UpdateStationCommand(StationId.of(id), code, name, city);
    }
}
