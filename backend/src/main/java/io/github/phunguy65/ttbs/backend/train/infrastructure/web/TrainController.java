package io.github.phunguy65.ttbs.backend.train.infrastructure.web;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResult;
import io.github.phunguy65.ttbs.backend.shared.domain.SortDirection;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.ErrorCode;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.FailData;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JsendResponse;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.SliceHttpResponse;
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
import io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.BulkSoftDeleteTrainsHttpRequest;
import io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.CreateTrainHttpRequest;
import io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.UpdateTrainHttpRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
class TrainController {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("createdAt", "trainNumber", "name", "totalSeats");

    private final CreateTrainUseCase createTrainUseCase;
    private final GetTrainByIdUseCase getTrainByIdUseCase;
    private final GetTrainsUseCase getTrainsUseCase;
    private final UpdateTrainUseCase updateTrainUseCase;
    private final SoftDeleteTrainUseCase softDeleteTrainUseCase;
    private final BulkSoftDeleteTrainsUseCase bulkSoftDeleteTrainsUseCase;
    private final TrainRequestMapper mapper;

    TrainController(
            CreateTrainUseCase createTrainUseCase,
            GetTrainByIdUseCase getTrainByIdUseCase,
            GetTrainsUseCase getTrainsUseCase,
            UpdateTrainUseCase updateTrainUseCase,
            SoftDeleteTrainUseCase softDeleteTrainUseCase,
            BulkSoftDeleteTrainsUseCase bulkSoftDeleteTrainsUseCase,
            TrainRequestMapper mapper) {
        this.createTrainUseCase = createTrainUseCase;
        this.getTrainByIdUseCase = getTrainByIdUseCase;
        this.getTrainsUseCase = getTrainsUseCase;
        this.updateTrainUseCase = updateTrainUseCase;
        this.softDeleteTrainUseCase = softDeleteTrainUseCase;
        this.bulkSoftDeleteTrainsUseCase = bulkSoftDeleteTrainsUseCase;
        this.mapper = mapper;
    }

    @PostMapping(value = "/{version}/trains", version = "1.0")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<JsendResponse<?>> create(@Valid @RequestBody CreateTrainHttpRequest request) {
        return createTrainUseCase
                .execute(mapper.toCommand(request))
                .fold(
                        dto -> {
                            var location = ServletUriComponentsBuilder.fromCurrentRequest()
                                    .path("/{id}")
                                    .buildAndExpand(dto.id())
                                    .toUri();
                            return ResponseEntity.created(location)
                                    .body(JsendResponse.success(mapper.toResponse(dto)));
                        },
                        error -> errorResponse(error));
    }

    @GetMapping(value = "/{version}/trains", version = "1.0")
    ResponseEntity<JsendResponse<?>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        if (page < 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(JsendResponse.fail(new FailData(
                            "page must be >= 0", ErrorCode.VALIDATION_ERROR, List.of())));
        }

        if (size < 1 || size > 100) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(JsendResponse.fail(new FailData(
                            "size must be between 1 and 100",
                            ErrorCode.VALIDATION_ERROR,
                            List.of())));
        }

        String[] sortParts = sort.split(",", 2);
        String sortField = sortParts[0].trim();
        SortDirection direction =
                (sortParts.length > 1 && "asc".equalsIgnoreCase(sortParts[1].trim()))
                        ? SortDirection.ASC
                        : SortDirection.DESC;

        if (!ALLOWED_SORT_FIELDS.contains(sortField)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(JsendResponse.fail(new FailData(
                            "sort field not allowed: " + sortField,
                            ErrorCode.VALIDATION_ERROR,
                            List.of())));
        }

        PageResult<TrainResponse> result =
                getTrainsUseCase.execute(page, size, sortField, direction);

        List<TrainHttpResponse> content =
                result.items().stream().map(mapper::toResponse).toList();

        SliceHttpResponse<TrainHttpResponse> sliceResponse = new SliceHttpResponse<>(
                content,
                result.pageNumber(),
                result.pageSize(),
                result.hasNext(),
                result.hasPrevious());

        return ResponseEntity.ok(JsendResponse.success(sliceResponse));
    }

    @GetMapping(value = "/{version}/trains/{id}", version = "1.0")
    ResponseEntity<JsendResponse<?>> getById(@PathVariable UUID id) {
        return getTrainByIdUseCase
                .execute(TrainId.of(id))
                .fold(
                        dto -> ResponseEntity.ok(JsendResponse.success(mapper.toResponse(dto))),
                        error -> errorResponse(error));
    }

    @PatchMapping(value = "/{version}/trains/{id}", version = "1.0")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<JsendResponse<?>> patchById(
            @PathVariable UUID id, @Valid @RequestBody UpdateTrainHttpRequest request) {
        return updateTrainUseCase
                .execute(mapper.toUpdateCommand(id, request))
                .fold(
                        dto -> ResponseEntity.ok(JsendResponse.success(mapper.toResponse(dto))),
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
            @Valid @RequestBody BulkSoftDeleteTrainsHttpRequest request) {
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
