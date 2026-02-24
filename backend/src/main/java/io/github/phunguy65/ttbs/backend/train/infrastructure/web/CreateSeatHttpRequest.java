package io.github.phunguy65.ttbs.backend.train.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record CreateSeatHttpRequest(
        @NotBlank(message = "Seat number is required") @Size(max = 10, message = "Seat number must not exceed 10 characters") String seatNumber,

        @NotNull(message = "Seat class is required") String seatClass) {}
