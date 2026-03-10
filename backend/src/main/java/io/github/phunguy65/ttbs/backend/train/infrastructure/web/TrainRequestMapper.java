package io.github.phunguy65.ttbs.backend.train.infrastructure.web;

import io.github.phunguy65.ttbs.backend.train.application.command.CreateTrainCommand;
import io.github.phunguy65.ttbs.backend.train.application.command.UpdateTrainCommand;
import io.github.phunguy65.ttbs.backend.train.application.dto.TrainDto;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class TrainRequestMapper {

    CreateTrainCommand toCommand(CreateTrainHttpRequest request) {
        return new CreateTrainCommand(request.trainNumber(), request.name(), request.totalSeats());
    }

    TrainHttpResponse toResponse(TrainDto dto) {
        return new TrainHttpResponse(
                dto.id(), dto.trainNumber(), dto.name(), dto.totalSeats(), dto.createdAt());
    }

    UpdateTrainCommand toUpdateCommand(UUID id, UpdateTrainHttpRequest request) {
        return new UpdateTrainCommand(
                TrainId.of(id), request.trainNumber(), request.name(), request.totalSeats());
    }
}
