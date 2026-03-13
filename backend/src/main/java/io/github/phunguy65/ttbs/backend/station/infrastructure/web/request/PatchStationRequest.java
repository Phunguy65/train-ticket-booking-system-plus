package io.github.phunguy65.ttbs.backend.station.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

record PatchStationRequest(
        @NotBlank @Size(max = 10) JsonNullable<String> code,
        @NotBlank @Size(max = 255) JsonNullable<String> name,
        @NotBlank @Size(max = 100) JsonNullable<String> city) {

    PatchStationRequest() {
        this(JsonNullable.undefined(), JsonNullable.undefined(), JsonNullable.undefined());
    }
}
