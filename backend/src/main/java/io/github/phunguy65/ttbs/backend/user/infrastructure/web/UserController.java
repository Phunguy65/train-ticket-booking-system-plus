package io.github.phunguy65.ttbs.backend.user.infrastructure.web;

import io.github.phunguy65.ttbs.backend.shared.domain.UserId;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.ErrorCode;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.FailData;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JsendResponse;
import io.github.phunguy65.ttbs.backend.user.application.usecase.CreateUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.GetUserByIdUseCase;
import io.github.phunguy65.ttbs.backend.user.domain.errors.UserError;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
class UserController {

    private final GetUserByIdUseCase getUserByIdUseCase;
    private final CreateUserUseCase createUserUseCase;
    private final UserRequestMapper mapper;

    UserController(
            GetUserByIdUseCase getUserByIdUseCase,
            CreateUserUseCase createUserUseCase,
            UserRequestMapper mapper) {
        this.getUserByIdUseCase = getUserByIdUseCase;
        this.createUserUseCase = createUserUseCase;
        this.mapper = mapper;
    }

    @PostMapping
    ResponseEntity<JsendResponse<?>> create(@Valid @RequestBody CreateUserHttpRequest request) {
        return createUserUseCase
                .execute(mapper.toCommand(request))
                .fold(
                        result -> {
                            URI location =
                                    URI.create("/api/v1/users/" + result.user().id());
                            return ResponseEntity.created(location)
                                    .body(JsendResponse.success(mapper.toCreateResponse(result)));
                        },
                        error -> errorResponse(error));
    }

    @GetMapping("/{id}")
    ResponseEntity<JsendResponse<?>> getById(@PathVariable UUID id) {
        return getUserByIdUseCase
                .execute(UserId.of(id))
                .fold(
                        userDto -> ResponseEntity.ok(
                                JsendResponse.success(mapper.toResponse(userDto))),
                        error -> errorResponse(error));
    }

    @GetMapping("/me")
    ResponseEntity<JsendResponse<?>> getMe() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID principalId = UUID.fromString(auth.getName());
        return getUserByIdUseCase
                .execute(UserId.of(principalId))
                .fold(
                        userDto -> ResponseEntity.ok(
                                JsendResponse.success(mapper.toResponse(userDto))),
                        error -> errorResponse(error));
    }

    private ResponseEntity<JsendResponse<?>> errorResponse(UserError error) {
        HttpStatus status =
                switch (error) {
                    case UserError.UserNotFound e -> HttpStatus.NOT_FOUND;
                    case UserError.EmailAlreadyExists e -> HttpStatus.CONFLICT;
                    case UserError.InvalidCredentials e -> HttpStatus.UNAUTHORIZED;
                    case UserError.InvalidRefreshToken e -> HttpStatus.UNAUTHORIZED;
                };
        ErrorCode code =
                switch (error) {
                    case UserError.UserNotFound e -> ErrorCode.USER_NOT_FOUND;
                    case UserError.EmailAlreadyExists e -> ErrorCode.USER_EMAIL_ALREADY_EXISTS;
                    case UserError.InvalidCredentials e -> ErrorCode.USER_INVALID_CREDENTIALS;
                    case UserError.InvalidRefreshToken e -> ErrorCode.USER_INVALID_REFRESH_TOKEN;
                };
        return ResponseEntity.status(status)
                .body(JsendResponse.fail(new FailData(error.message(), code, List.of())));
    }
}
