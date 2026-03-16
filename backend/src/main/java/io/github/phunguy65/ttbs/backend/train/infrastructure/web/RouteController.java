package io.github.phunguy65.ttbs.backend.train.infrastructure.web;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.ErrorCode;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.FailData;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JsendResponse;
import io.github.phunguy65.ttbs.backend.train.application.command.BulkSoftDeleteRoutesCommand;
import io.github.phunguy65.ttbs.backend.train.application.command.SoftDeleteRouteCommand;
import io.github.phunguy65.ttbs.backend.train.application.response.RouteResponse;
import io.github.phunguy65.ttbs.backend.train.application.usecase.BulkSoftDeleteRoutesUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.CreateRouteUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetRouteByIdUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetRoutesUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.SoftDeleteRouteUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.UpdateRouteUseCase;
import io.github.phunguy65.ttbs.backend.train.domain.error.RouteError;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.BulkSoftDeleteRoutesRequest;
import io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.CreateRouteRequest;
import io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.GetRoutesRequest;
import io.github.phunguy65.ttbs.backend.train.infrastructure.web.request.PatchRouteRequest;
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
class RouteController {

    private final CreateRouteUseCase createRouteUseCase;
    private final GetRouteByIdUseCase getRouteByIdUseCase;
    private final GetRoutesUseCase getRoutesUseCase;
    private final UpdateRouteUseCase updateRouteUseCase;
    private final SoftDeleteRouteUseCase softDeleteRouteUseCase;
    private final BulkSoftDeleteRoutesUseCase bulkSoftDeleteRoutesUseCase;

    RouteController(
            CreateRouteUseCase createRouteUseCase,
            GetRouteByIdUseCase getRouteByIdUseCase,
            GetRoutesUseCase getRoutesUseCase,
            UpdateRouteUseCase updateRouteUseCase,
            SoftDeleteRouteUseCase softDeleteRouteUseCase,
            BulkSoftDeleteRoutesUseCase bulkSoftDeleteRoutesUseCase) {
        this.createRouteUseCase = createRouteUseCase;
        this.getRouteByIdUseCase = getRouteByIdUseCase;
        this.getRoutesUseCase = getRoutesUseCase;
        this.updateRouteUseCase = updateRouteUseCase;
        this.softDeleteRouteUseCase = softDeleteRouteUseCase;
        this.bulkSoftDeleteRoutesUseCase = bulkSoftDeleteRoutesUseCase;
    }

    @PostMapping(value = "/{version}/routes", version = "1.0")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<JsendResponse<?>> create(@Valid @RequestBody CreateRouteRequest request) {
        return createRouteUseCase
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

    @GetMapping(value = "/{version}/routes", version = "1.0")
    ResponseEntity<JsendResponse<?>> list(@ModelAttribute @Valid GetRoutesRequest request) {
        PageResponse<RouteResponse> result = getRoutesUseCase.execute(request.toQuery());

        return ResponseEntity.ok(JsendResponse.success(result));
    }

    @GetMapping(value = "/{version}/routes/{id}", version = "1.0")
    ResponseEntity<JsendResponse<?>> getById(@PathVariable UUID id) {
        return getRouteByIdUseCase
                .execute(RouteId.of(id))
                .fold(
                        dto -> ResponseEntity.ok(JsendResponse.success(dto)),
                        error -> errorResponse(error));
    }

    @PatchMapping(value = "/{version}/routes/{id}", version = "1.0")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<JsendResponse<?>> patchById(
            @PathVariable UUID id, @Valid @RequestBody PatchRouteRequest request) {
        return updateRouteUseCase
                .execute(request.toCommand(id))
                .fold(
                        dto -> ResponseEntity.ok(JsendResponse.success(dto)),
                        error -> errorResponse(error));
    }

    @DeleteMapping(value = "/{version}/routes/{id}", version = "1.0")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<JsendResponse<?>> deleteById(@PathVariable UUID id) {
        return softDeleteRouteUseCase
                .execute(new SoftDeleteRouteCommand(RouteId.of(id)))
                .fold(
                        v -> ResponseEntity.ok(JsendResponse.success()),
                        error -> errorResponse(error));
    }

    @PostMapping(value = "/{version}/routes:bulkDelete", version = "1.0")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<JsendResponse<?>> bulkDelete(
            @Valid @RequestBody BulkSoftDeleteRoutesRequest request) {
        List<RouteId> routeIds = request.routeIds().stream().map(RouteId::of).toList();
        return bulkSoftDeleteRoutesUseCase
                .execute(new BulkSoftDeleteRoutesCommand(routeIds))
                .fold(
                        deletedCount -> ResponseEntity.ok(
                                JsendResponse.success(Map.of("deletedCount", deletedCount))),
                        error -> errorResponse(error));
    }

    private ResponseEntity<JsendResponse<?>> errorResponse(RouteError error) {
        HttpStatus status =
                switch (error) {
                    case RouteError.RouteNotFound e -> HttpStatus.NOT_FOUND;
                    case RouteError.RoutesNotFound e -> HttpStatus.UNPROCESSABLE_ENTITY;
                };
        ErrorCode code =
                switch (error) {
                    case RouteError.RouteNotFound e -> ErrorCode.ROUTE_NOT_FOUND;
                    case RouteError.RoutesNotFound e -> ErrorCode.ROUTES_NOT_FOUND;
                };
        return ResponseEntity.status(status)
                .body(JsendResponse.fail(new FailData(error.message(), code, List.of())));
    }
}
