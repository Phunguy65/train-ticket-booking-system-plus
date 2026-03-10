package io.github.phunguy65.ttbs.backend.train.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

record UpdateTrainHttpRequest(
        @NotBlank @Size(max = 20) JsonNullable<String> trainNumber,
        @NotBlank @Size(max = 255) JsonNullable<String> name,
        @Positive JsonNullable<Integer> totalSeats) {

    UpdateTrainHttpRequest() {
        this(JsonNullable.undefined(), JsonNullable.undefined(), JsonNullable.undefined());
    }
}
