package io.github.phunguy65.ttbs.backend.train.infrastructure.web;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.ErrorCode;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.FailData;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JsendResponse;
import io.github.phunguy65.ttbs.backend.train.application.response.TrainResponse;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetTrainByIdUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetTrainsUseCase;
import io.github.phunguy65.ttbs.backend.train.domain.error.TrainError;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.GetTrainsRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
class TrainController {

    private final GetTrainByIdUseCase getTrainByIdUseCase;
    private final GetTrainsUseCase getTrainsUseCase;

    TrainController(GetTrainByIdUseCase getTrainByIdUseCase, GetTrainsUseCase getTrainsUseCase) {
        this.getTrainByIdUseCase = getTrainByIdUseCase;
        this.getTrainsUseCase = getTrainsUseCase;
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
