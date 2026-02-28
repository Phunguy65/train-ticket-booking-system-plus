package io.github.phunguy65.ttbs.backend.train.infrastructure.web;

import io.github.phunguy65.ttbs.backend.train.application.command.CreateCoachCommand;
import io.github.phunguy65.ttbs.backend.train.application.dto.CoachDto;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class CoachRequestMapper {

    CreateCoachCommand toCommand(UUID trainId, CreateCoachHttpRequest request) {
        return new CreateCoachCommand(trainId, request.carNumber(), request.totalSeats());
    }

    CoachHttpResponse toResponse(CoachDto dto) {
        return new CoachHttpResponse(
                dto.id(), dto.trainId(), dto.carNumber(), dto.totalSeats(), dto.createdAt());
    }
}
