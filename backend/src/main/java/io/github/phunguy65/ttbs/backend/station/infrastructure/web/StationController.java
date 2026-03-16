package io.github.phunguy65.ttbs.backend.station.infrastructure.web;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.ErrorCode;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.FailData;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JsendResponse;
import io.github.phunguy65.ttbs.backend.station.application.command.BulkSoftDeleteStationsCommand;
import io.github.phunguy65.ttbs.backend.station.application.command.SoftDeleteStationCommand;
import io.github.phunguy65.ttbs.backend.station.application.response.StationResponse;
import io.github.phunguy65.ttbs.backend.station.application.usecase.BulkSoftDeleteStationsUseCase;
import io.github.phunguy65.ttbs.backend.station.application.usecase.CreateStationUseCase;
import io.github.phunguy65.ttbs.backend.station.application.usecase.GetStationByIdUseCase;
import io.github.phunguy65.ttbs.backend.station.application.usecase.GetStationsUseCase;
import io.github.phunguy65.ttbs.backend.station.application.usecase.SoftDeleteStationUseCase;
import io.github.phunguy65.ttbs.backend.station.application.usecase.UpdateStationUseCase;
import io.github.phunguy65.ttbs.backend.station.domain.error.StationError;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import io.github.phunguy65.ttbs.backend.station.infrastructure.web.request.BulkSoftDeleteStationsRequest;
import io.github.phunguy65.ttbs.backend.station.infrastructure.web.request.CreateStationRequest;
import io.github.phunguy65.ttbs.backend.station.infrastructure.web.request.GetStationsRequest;
import io.github.phunguy65.ttbs.backend.station.infrastructure.web.request.PatchStationRequest;
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
class StationController {

    private final CreateStationUseCase createStationUseCase;
    private final GetStationByIdUseCase getStationByIdUseCase;
    private final GetStationsUseCase getStationsUseCase;
    private final UpdateStationUseCase updateStationUseCase;
    private final SoftDeleteStationUseCase softDeleteStationUseCase;
    private final BulkSoftDeleteStationsUseCase bulkSoftDeleteStationsUseCase;

    StationController(
            CreateStationUseCase createStationUseCase,
            GetStationByIdUseCase getStationByIdUseCase,
            GetStationsUseCase getStationsUseCase,
            UpdateStationUseCase updateStationUseCase,
            SoftDeleteStationUseCase softDeleteStationUseCase,
            BulkSoftDeleteStationsUseCase bulkSoftDeleteStationsUseCase) {
        this.createStationUseCase = createStationUseCase;
        this.getStationByIdUseCase = getStationByIdUseCase;
        this.getStationsUseCase = getStationsUseCase;
        this.updateStationUseCase = updateStationUseCase;
        this.softDeleteStationUseCase = softDeleteStationUseCase;
        this.bulkSoftDeleteStationsUseCase = bulkSoftDeleteStationsUseCase;
    }

    @PostMapping(value = "/{version}/stations", version = "1.0")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<JsendResponse<?>> create(@Valid @RequestBody CreateStationRequest request) {
        return createStationUseCase
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

    @PatchMapping(value = "/{version}/stations/{id}", version = "1.0")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<JsendResponse<?>> patchById(
            @PathVariable UUID id, @Valid @RequestBody PatchStationRequest request) {
        return updateStationUseCase
                .execute(request.toCommand(id))
                .fold(
                        dto -> ResponseEntity.ok(JsendResponse.success(dto)),
                        error -> errorResponse(error));
    }

    @DeleteMapping(value = "/{version}/stations/{id}", version = "1.0")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<JsendResponse<?>> deleteById(@PathVariable UUID id) {
        return softDeleteStationUseCase
                .execute(new SoftDeleteStationCommand(StationId.of(id)))
                .fold(
                        v -> ResponseEntity.ok(JsendResponse.success()),
                        error -> errorResponse(error));
    }

    @PostMapping(value = "/{version}/stations:bulkDelete", version = "1.0")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<JsendResponse<?>> bulkDelete(
            @Valid @RequestBody BulkSoftDeleteStationsRequest request) {
        List<StationId> stationIds =
                request.stationIds().stream().map(StationId::of).toList();
        return bulkSoftDeleteStationsUseCase
                .execute(new BulkSoftDeleteStationsCommand(stationIds))
                .fold(
                        deletedCount -> ResponseEntity.ok(
                                JsendResponse.success(Map.of("deletedCount", deletedCount))),
                        error -> errorResponse(error));
    }

    private ResponseEntity<JsendResponse<?>> errorResponse(StationError error) {
        HttpStatus status =
                switch (error) {
                    case StationError.StationNotFound e -> HttpStatus.NOT_FOUND;
                    case StationError.StationCodeAlreadyExists e -> HttpStatus.CONFLICT;
                    case StationError.StationInUse e -> HttpStatus.UNPROCESSABLE_ENTITY;
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
