package io.github.phunguy65.ttbs.backend.user.infrastructure.web;

import io.github.phunguy65.ttbs.backend.shared.application.response.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.ErrorCode;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.FailData;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JsendResponse;
import io.github.phunguy65.ttbs.backend.user.application.command.BulkSoftDeleteUsersCommand;
import io.github.phunguy65.ttbs.backend.user.application.command.SoftDeleteUserCommand;
import io.github.phunguy65.ttbs.backend.user.application.response.UserResponse;
import io.github.phunguy65.ttbs.backend.user.application.usecase.BulkSoftDeleteUsersUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.CreateUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.GetUserByIdUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.ListUsersUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.SoftDeleteUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.UpdateUserUseCase;
import io.github.phunguy65.ttbs.backend.user.domain.error.UserError;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
class UserController {

    private final GetUserByIdUseCase getUserByIdUseCase;
    private final CreateUserUseCase createUserUseCase;
    private final ListUsersUseCase listUsersUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final SoftDeleteUserUseCase softDeleteUserUseCase;
    private final BulkSoftDeleteUsersUseCase bulkSoftDeleteUsersUseCase;

    UserController(
            GetUserByIdUseCase getUserByIdUseCase,
            CreateUserUseCase createUserUseCase,
            ListUsersUseCase listUsersUseCase,
            UpdateUserUseCase updateUserUseCase,
            SoftDeleteUserUseCase softDeleteUserUseCase,
            BulkSoftDeleteUsersUseCase bulkSoftDeleteUsersUseCase) {
        this.getUserByIdUseCase = getUserByIdUseCase;
        this.createUserUseCase = createUserUseCase;
        this.listUsersUseCase = listUsersUseCase;
        this.updateUserUseCase = updateUserUseCase;
        this.softDeleteUserUseCase = softDeleteUserUseCase;
        this.bulkSoftDeleteUsersUseCase = bulkSoftDeleteUsersUseCase;
    }

    @PostMapping(value = "/{version}/users", version = "1.0")
    ResponseEntity<JsendResponse<?>> create(@Valid @RequestBody CreateUserHttpRequest request) {
        return createUserUseCase
                .execute(request.toCommand())
                .fold(
                        result -> {
                            var location = ServletUriComponentsBuilder.fromCurrentRequest()
                                    .path("/{id}")
                                    .buildAndExpand(result.user().id())
                                    .toUri();
                            return ResponseEntity.created(location)
                                    .body(JsendResponse.success(result));
                        },
                        error -> errorResponse(error));
    }

    @GetMapping(value = "/{version}/users/{id}", version = "1.0")
    ResponseEntity<JsendResponse<?>> getById(@PathVariable UUID id) {
        return getUserByIdUseCase
                .execute(UserId.of(id))
                .fold(
                        userDto -> ResponseEntity.ok(JsendResponse.success(userDto)),
                        error -> errorResponse(error));
    }

    @GetMapping(value = "/{version}/users/me", version = "1.0")
    ResponseEntity<JsendResponse<?>> getMe() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID principalId = UUID.fromString(auth.getName());
        return getUserByIdUseCase
                .execute(UserId.of(principalId))
                .fold(
                        userDto -> ResponseEntity.ok(JsendResponse.success(userDto)),
                        error -> errorResponse(error));
    }

    @GetMapping(value = "/{version}/users", version = "1.0")
    ResponseEntity<JsendResponse<?>> listUsers(@ModelAttribute @Valid GetUsersRequest request) {
        PageResponse<UserResponse> result = listUsersUseCase.execute(request.toQuery());

        return ResponseEntity.ok(JsendResponse.success(result));
    }

    @PatchMapping(value = "/{version}/users/{id}", version = "1.0")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<JsendResponse<?>> patchById(
            @PathVariable UUID id, @Valid @RequestBody UpdateUserHttpRequest request) {
        return updateUserUseCase
                .execute(request.toCommand(id))
                .fold(
                        userDto -> ResponseEntity.ok(JsendResponse.success(userDto)),
                        error -> errorResponse(error));
    }

    @PatchMapping(value = "/{version}/users/me", version = "1.0")
    ResponseEntity<JsendResponse<?>> patchMe(@Valid @RequestBody UpdateUserHttpRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID principalId = UUID.fromString(auth.getName());
        return updateUserUseCase
                .execute(request.toCommand(principalId))
                .fold(
                        userDto -> ResponseEntity.ok(JsendResponse.success(userDto)),
                        error -> errorResponse(error));
    }

    @DeleteMapping(value = "/{version}/users/me", version = "1.0")
    ResponseEntity<JsendResponse<?>> deleteMe() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID principalId = UUID.fromString(auth.getName());
        return softDeleteUserUseCase
                .execute(new SoftDeleteUserCommand(UserId.of(principalId)))
                .fold(
                        ignored -> ResponseEntity.ok(JsendResponse.success()),
                        error -> errorResponse(error));
    }

    @DeleteMapping(value = "/{version}/users/{id}", version = "1.0")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<JsendResponse<?>> deleteById(@PathVariable UUID id) {
        return softDeleteUserUseCase
                .execute(new SoftDeleteUserCommand(UserId.of(id)))
                .fold(
                        ignored -> ResponseEntity.ok(JsendResponse.success()),
                        error -> errorResponse(error));
    }

    @PostMapping(value = "/{version}/users:bulkDelete", version = "1.0")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<JsendResponse<?>> bulkDelete(
            @Valid @RequestBody BulkSoftDeleteUsersHttpRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID callerId = UUID.fromString(auth.getName());

        if (request.userIds().contains(callerId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(JsendResponse.fail(new FailData(
                            "Admin cannot include their own ID in a bulk delete request",
                            ErrorCode.USER_CANNOT_BULK_DELETE_SELF,
                            List.of())));
        }

        List<UserId> userIds = request.userIds().stream().map(UserId::of).toList();
        Result<Integer, UserError> result =
                bulkSoftDeleteUsersUseCase.execute(new BulkSoftDeleteUsersCommand(userIds));

        return switch (result) {
            case Result.Success<Integer, UserError> s ->
                ResponseEntity.ok(JsendResponse.success(Map.of("deletedCount", s.value())));
            case Result.Failure<Integer, UserError> f -> errorResponse(f.error());
        };
    }

    private ResponseEntity<JsendResponse<?>> errorResponse(UserError error) {
        return switch (error) {
            case UserError.UserAlreadyDeleted e -> ResponseEntity.ok(JsendResponse.success());
            default -> {
                HttpStatus status =
                        switch (error) {
                            case UserError.UserNotFound e -> HttpStatus.NOT_FOUND;
                            case UserError.EmailAlreadyExists e -> HttpStatus.CONFLICT;
                            case UserError.InvalidCredentials e -> HttpStatus.UNAUTHORIZED;
                            case UserError.InvalidRefreshToken e -> HttpStatus.UNAUTHORIZED;
                            case UserError.UserAlreadyDeleted e -> HttpStatus.OK; // unreachable
                            case UserError.UserHasActiveBookings e -> HttpStatus.CONFLICT;
                        };
                ErrorCode code =
                        switch (error) {
                            case UserError.UserNotFound e -> ErrorCode.USER_NOT_FOUND;
                            case UserError.EmailAlreadyExists e ->
                                ErrorCode.USER_EMAIL_ALREADY_EXISTS;
                            case UserError.InvalidCredentials e ->
                                ErrorCode.USER_INVALID_CREDENTIALS;
                            case UserError.InvalidRefreshToken e ->
                                ErrorCode.USER_INVALID_REFRESH_TOKEN;
                            case UserError.UserAlreadyDeleted e ->
                                ErrorCode.USER_NOT_FOUND; // unreachable
                            case UserError.UserHasActiveBookings e ->
                                ErrorCode.USER_HAS_ACTIVE_BOOKINGS;
                        };
                yield ResponseEntity.status(status)
                        .body(JsendResponse.fail(new FailData(error.message(), code, List.of())));
            }
        };
    }
}
