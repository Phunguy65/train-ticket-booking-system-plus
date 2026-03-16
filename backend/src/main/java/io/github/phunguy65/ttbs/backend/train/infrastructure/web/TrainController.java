package io.github.phunguy65.ttbs.backend.train.infrastructure.web;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.ErrorCode;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.FailData;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JsendResponse;
import io.github.phunguy65.ttbs.backend.train.application.command.BulkSoftDeleteTrainsCommand;
import io.github.phunguy65.ttbs.backend.train.application.command.SoftDeleteTrainCommand;
import io.github.phunguy65.ttbs.backend.train.application.response.TrainResponse;
import io.github.phunguy65.ttbs.backend.train.application.usecase.BulkSoftDeleteTrainsUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.CreateTrainUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetTrainByIdUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetTrainsUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.SoftDeleteTrainUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.UpdateTrainUseCase;
import io.github.phunguy65.ttbs.backend.train.domain.error.TrainError;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.BulkSoftDeleteTrainsRequest;
import io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.CreateTrainRequest;
import io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.GetTrainsRequest;
import io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.PatchTrainRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
class TrainController {

    private final CreateTrainUseCase createTrainUseCase;
    private final GetTrainByIdUseCase getTrainByIdUseCase;
    private final GetTrainsUseCase getTrainsUseCase;
    private final UpdateTrainUseCase updateTrainUseCase;
    private final SoftDeleteTrainUseCase softDeleteTrainUseCase;
    private final BulkSoftDeleteTrainsUseCase bulkSoftDeleteTrainsUseCase;

    TrainController(
            CreateTrainUseCase createTrainUseCase,
            GetTrainByIdUseCase getTrainByIdUseCase,
            GetTrainsUseCase getTrainsUseCase,
            UpdateTrainUseCase updateTrainUseCase,
            SoftDeleteTrainUseCase softDeleteTrainUseCase,
            BulkSoftDeleteTrainsUseCase bulkSoftDeleteTrainsUseCase) {
        this.createTrainUseCase = createTrainUseCase;
        this.getTrainByIdUseCase = getTrainByIdUseCase;
        this.getTrainsUseCase = getTrainsUseCase;
        this.updateTrainUseCase = updateTrainUseCase;
        this.softDeleteTrainUseCase = softDeleteTrainUseCase;
        this.bulkSoftDeleteTrainsUseCase = bulkSoftDeleteTrainsUseCase;
    }

    @PostMapping(value = "/{version}/trains", version = "1.0")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<JsendResponse<?>> create(@Valid @RequestBody CreateTrainRequest request) {
        return createTrainUseCase
                .execute(request.toCommand())
                .fold(
                        dto -> {
                            var location = ServletUriComponentsBuilder.fromCurrentRequest()
                                    .path("/{id}")
                                    .buildAndExpand(dto.id())
                                    .toUri();
                            return ResponseEntity.created(location)
                                    .body(JsendResponse.success(dto));
                        },
                        error -> errorResponse(error));
    }

    @GetMapping(value = "/{version}/trains", version = "1.0")
    ResponseEntity<JsendResponse<?>> list(@ModelAttribute @Valid GetTrainsRequest request) {
        PageResponse<TrainResponse> result = getTrainsUseCase.execute(request.toQuery());

        return ResponseEntity.ok(JsendResponse.success(result));
    }

    @GetMapping(value = "/{version}/trains/{id}", version = "1.0")
    ResponseEntity<JsendResponse<?>> getById(@PathVariable UUID id) {
        return getTrainByIdUseCase
                .execute(TrainId.of(id))
                .fold(
                        dto -> ResponseEntity.ok(JsendResponse.success(dto)),
                        error -> errorResponse(error));
    }

    @PatchMapping(value = "/{version}/trains/{id}", version = "1.0")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<JsendResponse<?>> patchById(
            @PathVariable UUID id, @Valid @RequestBody PatchTrainRequest request) {
        return updateTrainUseCase
                .execute(request.toCommand(id))
                .fold(
                        dto -> ResponseEntity.ok(JsendResponse.success(dto)),
                        error -> errorResponse(error));
    }

    @DeleteMapping(value = "/{version}/trains/{id}", version = "1.0")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<JsendResponse<?>> deleteById(@PathVariable UUID id) {
        return softDeleteTrainUseCase
                .execute(new SoftDeleteTrainCommand(TrainId.of(id)))
                .fold(
                        v -> ResponseEntity.ok(JsendResponse.success()),
                        error -> errorResponse(error));
    }

    @PostMapping(value = "/{version}/trains:bulkDelete", version = "1.0")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<JsendResponse<?>> bulkDelete(
            @Valid @RequestBody BulkSoftDeleteTrainsRequest request) {
        List<TrainId> trainIds = request.trainIds().stream().map(TrainId::of).toList();
        return bulkSoftDeleteTrainsUseCase
                .execute(new BulkSoftDeleteTrainsCommand(trainIds))
                .fold(
                        deletedCount -> ResponseEntity.ok(
                                JsendResponse.success(Map.of("deletedCount", deletedCount))),
                        error -> errorResponse(error));
    }

    private ResponseEntity<JsendResponse<?>> errorResponse(TrainError error) {
        HttpStatus status =
                switch (error) {
                    case TrainError.TrainNotFound e -> HttpStatus.NOT_FOUND;
                    case TrainError.TrainNumberAlreadyExists e -> HttpStatus.CONFLICT;
                    case TrainError.TrainInUse e -> HttpStatus.UNPROCESSABLE_ENTITY;
                };
        ErrorCode code =
                switch (error) {
                    case TrainError.TrainNotFound e -> ErrorCode.TRAIN_NOT_FOUND;
                    case TrainError.TrainNumberAlreadyExists e ->
                        ErrorCode.TRAIN_NUMBER_ALREADY_EXISTS;
                    case TrainError.TrainInUse e -> ErrorCode.TRAIN_IN_USE;
                };
        return ResponseEntity.status(status)
                .body(JsendResponse.fail(new FailData(error.message(), code, List.of())));
    }
}
