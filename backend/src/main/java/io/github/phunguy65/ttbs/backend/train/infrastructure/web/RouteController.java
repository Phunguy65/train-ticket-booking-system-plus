package io.github.phunguy65.ttbs.backend.train.infrastructure.web;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResult;
import io.github.phunguy65.ttbs.backend.shared.domain.SortDirection;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.ErrorCode;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.FailData;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JsendResponse;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.SliceHttpResponse;
import io.github.phunguy65.ttbs.backend.train.application.dto.RouteDto;
import io.github.phunguy65.ttbs.backend.train.application.usecase.CreateRouteUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetRouteByIdUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetRoutesUseCase;
import io.github.phunguy65.ttbs.backend.train.domain.errors.RouteError;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteFilter;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/{version}/routes")
class RouteController {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("createdAt", "departureTime", "arrivalTime", "basePrice");

    private final CreateRouteUseCase createRouteUseCase;
    private final GetRouteByIdUseCase getRouteByIdUseCase;
    private final GetRoutesUseCase getRoutesUseCase;
    private final RouteRequestMapper mapper;

    RouteController(
            CreateRouteUseCase createRouteUseCase,
            GetRouteByIdUseCase getRouteByIdUseCase,
            GetRoutesUseCase getRoutesUseCase,
            RouteRequestMapper mapper) {
        this.createRouteUseCase = createRouteUseCase;
        this.getRouteByIdUseCase = getRouteByIdUseCase;
        this.getRoutesUseCase = getRoutesUseCase;
        this.mapper = mapper;
    }

    @PostMapping(version = "1.0")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<JsendResponse<?>> create(@Valid @RequestBody CreateRouteHttpRequest request) {
        return createRouteUseCase
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

    @GetMapping(version = "1.0")
    ResponseEntity<JsendResponse<?>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false) UUID originStationId,
            @RequestParam(required = false) UUID destinationStationId,
            @RequestParam(required = false) Instant departureDateFrom,
            @RequestParam(required = false) Instant departureDateTo) {

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

        RouteFilter filter = new RouteFilter(
                originStationId, destinationStationId, departureDateFrom, departureDateTo);
        PageResult<RouteDto> result =
                getRoutesUseCase.execute(page, size, sortField, direction, filter);

        List<RouteHttpResponse> content =
                result.items().stream().map(mapper::toResponse).toList();

        SliceHttpResponse<RouteHttpResponse> sliceResponse = new SliceHttpResponse<>(
                content,
                result.pageNumber(),
                result.pageSize(),
                result.hasNext(),
                result.hasPrevious());

        return ResponseEntity.ok(JsendResponse.success(sliceResponse));
    }

    @GetMapping(value = "/{id}", version = "1.0")
    ResponseEntity<JsendResponse<?>> getById(@PathVariable UUID id) {
        return getRouteByIdUseCase
                .execute(RouteId.of(id))
                .fold(
                        dto -> ResponseEntity.ok(JsendResponse.success(mapper.toResponse(dto))),
                        error -> errorResponse(error));
    }

    private ResponseEntity<JsendResponse<?>> errorResponse(RouteError error) {
        HttpStatus status =
                switch (error) {
                    case RouteError.RouteNotFound e -> HttpStatus.NOT_FOUND;
                };
        ErrorCode code =
                switch (error) {
                    case RouteError.RouteNotFound e -> ErrorCode.ROUTE_NOT_FOUND;
                };
        return ResponseEntity.status(status)
                .body(JsendResponse.fail(new FailData(error.message(), code, List.of())));
    }
}
