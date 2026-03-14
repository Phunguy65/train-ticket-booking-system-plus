package io.github.phunguy65.ttbs.backend.train.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.train.application.command.CreateSeatCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateSeatRequest(
        @NotBlank(message = "Seat number is required") @Size(max = 10, message = "Seat number must not exceed 10 characters") String seatNumber) {

    public CreateSeatCommand toCommand(UUID coachId) {
        return new CreateSeatCommand(coachId, seatNumber);
    }
}
