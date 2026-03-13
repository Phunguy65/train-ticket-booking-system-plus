package io.github.phunguy65.ttbs.backend.station.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record CreateStationRequest(
        @NotBlank(message = "Code is required") @Size(max = 10, message = "Code must not exceed 10 characters") String code,

        @NotBlank(message = "Name is required") @Size(max = 255, message = "Name must not exceed 255 characters") String name,

        @NotBlank(message = "City is required") @Size(max = 100, message = "City must not exceed 100 characters") String city) {}
