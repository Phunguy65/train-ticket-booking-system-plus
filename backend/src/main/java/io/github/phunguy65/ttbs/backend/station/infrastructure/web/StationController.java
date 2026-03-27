package io.github.phunguy65.ttbs.backend.station.infrastructure.web;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.ErrorCode;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.FailData;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JsendResponse;
import io.github.phunguy65.ttbs.backend.station.application.response.StationResponse;
import io.github.phunguy65.ttbs.backend.station.application.usecase.GetStationByIdUseCase;
import io.github.phunguy65.ttbs.backend.station.application.usecase.GetStationsUseCase;
import io.github.phunguy65.ttbs.backend.station.domain.error.StationError;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import io.github.phunguy65.ttbs.backend.station.infrastructure.web.request.GetStationsRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
class StationController {

    private final GetStationByIdUseCase getStationByIdUseCase;
    private final GetStationsUseCase getStationsUseCase;

    StationController(
            GetStationByIdUseCase getStationByIdUseCase, GetStationsUseCase getStationsUseCase) {
        this.getStationByIdUseCase = getStationByIdUseCase;
        this.getStationsUseCase = getStationsUseCase;
    }

    @GetMapping(value = "/{version}/stations", version = "1.0")
    ResponseEntity<JsendResponse<?>> list(@ModelAttribute @Valid GetStationsRequest request) {
        PageResponse<StationResponse> result = getStationsUseCase.execute(request.toQuery());

        return ResponseEntity.ok(JsendResponse.success(result));
    }

    @GetMapping(value = "/{version}/stations/{id}", version = "1.0")
    ResponseEntity<JsendResponse<?>> getById(@PathVariable UUID id) {
        return getStationByIdUseCase
                .execute(StationId.of(id))
                .fold(
                        dto -> ResponseEntity.ok(JsendResponse.success(dto)),
                        error -> errorResponse(error));
    }

    private ResponseEntity<JsendResponse<?>> errorResponse(StationError error) {
        HttpStatus status =
                switch (error) {
                    case StationError.StationNotFound e -> HttpStatus.NOT_FOUND;
                    case StationError.StationCodeAlreadyExists e -> HttpStatus.CONFLICT;
                    case StationError.StationInUse e -> HttpStatus.UNPROCESSABLE_CONTENT;
                };
        ErrorCode code =
                switch (error) {
                    case StationError.StationNotFound e -> ErrorCode.STATION_NOT_FOUND;
                    case StationError.StationCodeAlreadyExists e ->
                        ErrorCode.STATION_CODE_ALREADY_EXISTS;
                    case StationError.StationInUse e -> ErrorCode.STATION_IN_USE;
                };
        return ResponseEntity.status(status)
                .body(JsendResponse.fail(new FailData(error.message(), code, List.of())));
    }
}
