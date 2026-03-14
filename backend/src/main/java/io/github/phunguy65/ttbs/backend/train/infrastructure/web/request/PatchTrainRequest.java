package io.github.phunguy65.ttbs.backend.train.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.train.application.command.UpdateTrainCommand;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.openapitools.jackson.nullable.JsonNullable;

public record PatchTrainRequest(
        @NotBlank @Size(max = 20) JsonNullable<String> trainNumber,
        @NotBlank @Size(max = 255) JsonNullable<String> name,
        @Positive JsonNullable<Integer> totalSeats) {

    PatchTrainRequest() {
        this(JsonNullable.undefined(), JsonNullable.undefined(), JsonNullable.undefined());
    }

    public UpdateTrainCommand toCommand(UUID id) {
        return new UpdateTrainCommand(TrainId.of(id), trainNumber, name, totalSeats);
    }
}
