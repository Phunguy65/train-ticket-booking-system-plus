package io.github.phunguy65.ttbs.backend.station.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

record UpdateStationHttpRequest(
        @NotBlank @Size(max = 10) JsonNullable<String> code,
        @NotBlank @Size(max = 255) JsonNullable<String> name,
        @NotBlank @Size(max = 100) JsonNullable<String> city) {

    UpdateStationHttpRequest() {
        this(JsonNullable.undefined(), JsonNullable.undefined(), JsonNullable.undefined());
    }
}
