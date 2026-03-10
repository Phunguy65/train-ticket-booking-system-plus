package io.github.phunguy65.ttbs.backend.user.infrastructure.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.openapitools.jackson.nullable.JsonNullable;

record UpdateUserHttpRequest(
        @NotBlank JsonNullable<String> fullName,
        @NotBlank @Email JsonNullable<String> email,
        JsonNullable<String> phone) {

    UpdateUserHttpRequest() {
        this(JsonNullable.undefined(), JsonNullable.undefined(), JsonNullable.undefined());
    }
}
