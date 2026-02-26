package io.github.phunguy65.ttbs.backend.train.application.command;

import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateTrainCommand(
        TrainId trainId,
        JsonNullable<String> trainNumber,
        JsonNullable<String> name,
        JsonNullable<Integer> totalSeats) {}
