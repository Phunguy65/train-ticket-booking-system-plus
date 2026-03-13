package io.github.phunguy65.ttbs.backend.train.infrastructure.web;

import io.github.phunguy65.ttbs.backend.train.application.command.BulkCreateCoachesCommand;
import io.github.phunguy65.ttbs.backend.train.application.command.CreateCoachCommand;
import io.github.phunguy65.ttbs.backend.train.application.response.CoachResponse;
import io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.BulkCreateCoachesHttpRequest;
import io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.CreateCoachHttpRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class CoachRequestMapper {

    CreateCoachCommand toCommand(UUID trainId, CreateCoachHttpRequest request) {
        return new CreateCoachCommand(trainId, request.carNumber(), request.totalSeats());
    }

    BulkCreateCoachesCommand toBulkCommand(UUID trainId, BulkCreateCoachesHttpRequest request) {
        List<BulkCreateCoachesCommand.CoachItem> items = request.coaches().stream()
                .map(c -> new BulkCreateCoachesCommand.CoachItem(c.carNumber(), c.totalSeats()))
                .toList();
        return new BulkCreateCoachesCommand(trainId, items);
    }

    CoachHttpResponse toResponse(CoachResponse dto) {
        return new CoachHttpResponse(
                dto.id(), dto.trainId(), dto.carNumber(), dto.totalSeats(), dto.createdAt());
    }

    List<CoachHttpResponse> toResponseList(List<CoachResponse> dtos) {
        return dtos.stream().map(this::toResponse).toList();
    }
}
