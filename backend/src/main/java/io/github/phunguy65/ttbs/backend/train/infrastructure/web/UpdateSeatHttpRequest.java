package io.github.phunguy65.ttbs.backend.train.infrastructure.web;

import io.github.phunguy65.ttbs.backend.train.domain.model.SeatClass;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

record UpdateSeatHttpRequest(
        @NotBlank @Size(max = 10) JsonNullable<String> seatNumber,
        JsonNullable<SeatClass> seatClass) {

    UpdateSeatHttpRequest() {
        this(JsonNullable.undefined(), JsonNullable.undefined());
    }
}
