package io.github.phunguy65.ttbs.backend.train.infrastructure.web;

import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.ErrorCode;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.FailData;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JsendResponse;
import io.github.phunguy65.ttbs.backend.train.application.usecase.CreateCoachUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetCoachByIdUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetCoachesByTrainUseCase;
import io.github.phunguy65.ttbs.backend.train.domain.errors.CoachError;
import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
class CoachController {

    private final CreateCoachUseCase createCoachUseCase;
    private final GetCoachByIdUseCase getCoachByIdUseCase;
    private final GetCoachesByTrainUseCase getCoachesByTrainUseCase;
    private final CoachRequestMapper mapper;

    CoachController(
            CreateCoachUseCase createCoachUseCase,
            GetCoachByIdUseCase getCoachByIdUseCase,
            GetCoachesByTrainUseCase getCoachesByTrainUseCase,
            CoachRequestMapper mapper) {
        this.createCoachUseCase = createCoachUseCase;
        this.getCoachByIdUseCase = getCoachByIdUseCase;
        this.getCoachesByTrainUseCase = getCoachesByTrainUseCase;
        this.mapper = mapper;
    }

    @PostMapping(value = "/{version}/trains/{trainId}/coaches", version = "1.0")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<JsendResponse<?>> createCoach(
            @PathVariable UUID trainId, @Valid @RequestBody CreateCoachHttpRequest request) {
        return createCoachUseCase
                .execute(mapper.toCommand(trainId, request))
                .fold(
                        dto -> {
                            var location = ServletUriComponentsBuilder.fromCurrentRequest()
                                    .path("/{id}")
                                    .buildAndExpand(dto.id())
                                    .toUri();
                            return ResponseEntity.created(location)
                                    .body(JsendResponse.success(mapper.toResponse(dto)));
                        },
                        this::coachErrorResponse);
    }

    @GetMapping(value = "/{version}/trains/{trainId}/coaches", version = "1.0")
    ResponseEntity<JsendResponse<?>> getCoachesByTrain(@PathVariable UUID trainId) {
        List<CoachHttpResponse> responses =
                getCoachesByTrainUseCase.execute(TrainId.of(trainId)).stream()
                        .map(mapper::toResponse)
                        .toList();
        return ResponseEntity.ok(JsendResponse.success(responses));
    }

    @GetMapping(value = "/{version}/trains/{trainId}/coaches/{id}", version = "1.0")
    ResponseEntity<JsendResponse<?>> getCoachById(
            @PathVariable UUID trainId, @PathVariable UUID id) {
        return getCoachByIdUseCase
                .execute(CoachId.of(id), TrainId.of(trainId))
                .fold(
                        dto -> ResponseEntity.ok(JsendResponse.success(mapper.toResponse(dto))),
                        this::coachErrorResponse);
    }

    private ResponseEntity<JsendResponse<?>> coachErrorResponse(CoachError error) {
        HttpStatus status =
                switch (error) {
                    case CoachError.CoachNotFound e -> HttpStatus.NOT_FOUND;
                    case CoachError.CarNumberAlreadyExists e -> HttpStatus.CONFLICT;
                    case CoachError.TrainNotFound e -> HttpStatus.NOT_FOUND;
                };
        ErrorCode code =
                switch (error) {
                    case CoachError.CoachNotFound e -> ErrorCode.COACH_NOT_FOUND;
                    case CoachError.CarNumberAlreadyExists e ->
                        ErrorCode.COACH_CAR_NUMBER_ALREADY_EXISTS;
                    case CoachError.TrainNotFound e -> ErrorCode.COACH_TRAIN_NOT_FOUND;
                };
        return ResponseEntity.status(status)
                .body(JsendResponse.fail(new FailData(error.message(), code, List.of())));
    }
}
