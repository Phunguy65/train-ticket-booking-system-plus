package io.github.phunguy65.ttbs.backend.train.infrastructure.web;

import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.ErrorCode;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.FailData;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JsendResponse;
import io.github.phunguy65.ttbs.backend.train.application.command.BulkSoftDeleteCoachesCommand;
import io.github.phunguy65.ttbs.backend.train.application.command.SoftDeleteCoachCommand;
import io.github.phunguy65.ttbs.backend.train.application.usecase.BulkCreateCoachesUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.BulkSoftDeleteCoachesUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.CreateCoachUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetCoachByIdUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetCoachesByTrainUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.SoftDeleteCoachUseCase;
import io.github.phunguy65.ttbs.backend.train.domain.errors.CoachError;
import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
class CoachController {

    private final CreateCoachUseCase createCoachUseCase;
    private final GetCoachByIdUseCase getCoachByIdUseCase;
    private final GetCoachesByTrainUseCase getCoachesByTrainUseCase;
    private final SoftDeleteCoachUseCase softDeleteCoachUseCase;
    private final BulkSoftDeleteCoachesUseCase bulkSoftDeleteCoachesUseCase;
    private final BulkCreateCoachesUseCase bulkCreateCoachesUseCase;
    private final CoachRequestMapper mapper;

    CoachController(
            CreateCoachUseCase createCoachUseCase,
            GetCoachByIdUseCase getCoachByIdUseCase,
            GetCoachesByTrainUseCase getCoachesByTrainUseCase,
            SoftDeleteCoachUseCase softDeleteCoachUseCase,
            BulkSoftDeleteCoachesUseCase bulkSoftDeleteCoachesUseCase,
            BulkCreateCoachesUseCase bulkCreateCoachesUseCase,
            CoachRequestMapper mapper) {
        this.createCoachUseCase = createCoachUseCase;
        this.getCoachByIdUseCase = getCoachByIdUseCase;
        this.getCoachesByTrainUseCase = getCoachesByTrainUseCase;
        this.softDeleteCoachUseCase = softDeleteCoachUseCase;
        this.bulkSoftDeleteCoachesUseCase = bulkSoftDeleteCoachesUseCase;
        this.bulkCreateCoachesUseCase = bulkCreateCoachesUseCase;
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

    @DeleteMapping(value = "/{version}/trains/{trainId}/coaches/{id}", version = "1.0")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<JsendResponse<?>> deleteById(@PathVariable UUID trainId, @PathVariable UUID id) {
        return softDeleteCoachUseCase
                .execute(new SoftDeleteCoachCommand(CoachId.of(id), TrainId.of(trainId)))
                .fold(v -> ResponseEntity.ok(JsendResponse.success()), this::coachErrorResponse);
    }

    @DeleteMapping(value = "/{version}/coaches", version = "1.0")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<JsendResponse<?>> bulkDelete(
            @RequestParam(value = "ids", required = false) List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(JsendResponse.fail(new FailData(
                            "At least one coach ID is required",
                            ErrorCode.VALIDATION_ERROR,
                            List.of())));
        }
        if (ids.size() > 100) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(JsendResponse.fail(new FailData(
                            "Bulk delete is limited to 100 IDs per request",
                            ErrorCode.VALIDATION_ERROR,
                            List.of())));
        }
        List<CoachId> coachIds = ids.stream().map(CoachId::of).toList();
        return bulkSoftDeleteCoachesUseCase
                .execute(new BulkSoftDeleteCoachesCommand(coachIds))
                .fold(
                        deletedCount -> ResponseEntity.ok(
                                JsendResponse.success(Map.of("deletedCount", deletedCount))),
                        this::coachErrorResponse);
    }

    @PostMapping(value = "/{version}/trains/{trainId}/coaches:bulkCreate", version = "1.0")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<JsendResponse<?>> bulkCreateCoaches(
            @PathVariable UUID trainId, @Valid @RequestBody BulkCreateCoachesHttpRequest request) {
        return bulkCreateCoachesUseCase
                .execute(mapper.toBulkCommand(trainId, request))
                .fold(
                        dtos -> ResponseEntity.status(HttpStatus.CREATED)
                                .body(JsendResponse.success(mapper.toResponseList(dtos))),
                        this::coachErrorResponse);
    }

    private ResponseEntity<JsendResponse<?>> coachErrorResponse(CoachError error) {
        HttpStatus status =
                switch (error) {
                    case CoachError.CoachNotFound e -> HttpStatus.NOT_FOUND;
                    case CoachError.CarNumberAlreadyExists e -> HttpStatus.CONFLICT;
                    case CoachError.TrainNotFound e -> HttpStatus.NOT_FOUND;
                    case CoachError.CoachInUse e -> HttpStatus.UNPROCESSABLE_ENTITY;
                    case CoachError.CarNumbersAlreadyExist e -> HttpStatus.CONFLICT;
                    case CoachError.DuplicateCarNumbersInRequest e ->
                        HttpStatus.UNPROCESSABLE_ENTITY;
                };
        ErrorCode code =
                switch (error) {
                    case CoachError.CoachNotFound e -> ErrorCode.COACH_NOT_FOUND;
                    case CoachError.CarNumberAlreadyExists e ->
                        ErrorCode.COACH_CAR_NUMBER_ALREADY_EXISTS;
                    case CoachError.TrainNotFound e -> ErrorCode.COACH_TRAIN_NOT_FOUND;
                    case CoachError.CoachInUse e -> ErrorCode.COACH_IN_USE;
                    case CoachError.CarNumbersAlreadyExist e ->
                        ErrorCode.COACH_CAR_NUMBERS_ALREADY_EXIST;
                    case CoachError.DuplicateCarNumbersInRequest e ->
                        ErrorCode.COACH_DUPLICATE_CAR_NUMBERS_IN_REQUEST;
                };

        if (error instanceof CoachError.CoachInUse inUse) {
            return ResponseEntity.status(status)
                    .body(JsendResponse.fail(Map.of(
                            "message", inUse.message(),
                            "code", code,
                            "conflictingIds", inUse.conflictingIds())));
        }

        if (error instanceof CoachError.CarNumbersAlreadyExist conflict) {
            return ResponseEntity.status(status)
                    .body(JsendResponse.fail(Map.of(
                            "message", conflict.message(),
                            "code", code,
                            "conflictingCarNumbers", conflict.conflictingCarNumbers())));
        }

        if (error instanceof CoachError.DuplicateCarNumbersInRequest dup) {
            return ResponseEntity.status(status)
                    .body(JsendResponse.fail(Map.of(
                            "message", dup.message(),
                            "code", code,
                            "duplicates", dup.duplicates())));
        }

        return ResponseEntity.status(status)
                .body(JsendResponse.fail(new FailData(error.message(), code, List.of())));
    }
}
