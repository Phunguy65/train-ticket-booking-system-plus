package io.github.phunguy65.ttbs.backend.train.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

record CreateTrainHttpRequest(
        @NotBlank(message = "Train number is required") @Size(max = 20, message = "Train number must not exceed 20 characters") String trainNumber,

        @NotBlank(message = "Name is required") @Size(max = 255, message = "Name must not exceed 255 characters") String name,

        @Positive(message = "Total seats must be a positive number") int totalSeats) {}
