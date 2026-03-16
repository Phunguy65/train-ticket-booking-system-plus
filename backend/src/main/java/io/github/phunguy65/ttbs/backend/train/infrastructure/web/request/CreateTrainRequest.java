package io.github.phunguy65.ttbs.backend.train.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.train.application.command.CreateTrainCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTrainRequest(
        @NotBlank(message = "Train number is required") @Size(max = 20, message = "Train number must not exceed 20 characters") String trainNumber,

        @NotBlank(message = "Name is required") @Size(max = 255, message = "Name must not exceed 255 characters") String name) {

    public CreateTrainCommand toCommand() {
        return new CreateTrainCommand(trainNumber, name);
    }
}
