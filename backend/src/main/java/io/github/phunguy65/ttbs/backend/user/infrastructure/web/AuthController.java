package io.github.phunguy65.ttbs.backend.user.infrastructure.web;

import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.ErrorCode;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.FailData;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JsendResponse;
import io.github.phunguy65.ttbs.backend.user.application.usecase.LoginUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.LogoutUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.RefreshTokenUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.RegisterUserUseCase;
import io.github.phunguy65.ttbs.backend.user.domain.error.UserError;
import io.github.phunguy65.ttbs.backend.user.infrastructure.web.request.LoginRequest;
import io.github.phunguy65.ttbs.backend.user.infrastructure.web.request.RefreshTokenRequest;
import io.github.phunguy65.ttbs.backend.user.infrastructure.web.request.RegisterRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/{version}/auth")
class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUserUseCase loginUserUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUserUseCase logoutUserUseCase;

    AuthController(
            RegisterUserUseCase registerUserUseCase,
            LoginUserUseCase loginUserUseCase,
            RefreshTokenUseCase refreshTokenUseCase,
            LogoutUserUseCase logoutUserUseCase) {
        this.registerUserUseCase = registerUserUseCase;
        this.loginUserUseCase = loginUserUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUserUseCase = logoutUserUseCase;
    }

    @PostMapping(value = "/register", version = "1.0")
    ResponseEntity<JsendResponse<?>> register(@Valid @RequestBody RegisterRequest request) {
        return registerUserUseCase
                .execute(request.toCommand())
                .fold(
                        userDto -> {
                            var location = ServletUriComponentsBuilder.fromCurrentRequest()
                                    .build()
                                    .toUri();
                            return ResponseEntity.created(location)
                                    .body(JsendResponse.success(userDto));
                        },
                        error -> errorResponse(error));
    }

    @PostMapping(value = "/login", version = "1.0")
    ResponseEntity<JsendResponse<?>> login(@Valid @RequestBody LoginRequest request) {
        return loginUserUseCase
                .execute(request.toCommand())
                .fold(
                        loginResult -> ResponseEntity.ok(JsendResponse.success(loginResult)),
                        error -> errorResponse(error));
    }

    @PostMapping(value = "/refresh", version = "1.0")
    ResponseEntity<JsendResponse<?>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return refreshTokenUseCase
                .execute(request.toCommand())
                .fold(
                        loginResult -> ResponseEntity.ok(JsendResponse.success(loginResult)),
                        error -> errorResponse(error));
    }

    @PostMapping(value = "/logout", version = "1.0")
    ResponseEntity<JsendResponse<?>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        return logoutUserUseCase
                .execute(request.refreshToken())
                .fold(
                        ignored -> ResponseEntity.ok(JsendResponse.success()),
                        error -> errorResponse(error));
    }

    private ResponseEntity<JsendResponse<?>> errorResponse(UserError error) {
        HttpStatus status =
                switch (error) {
                    case UserError.EmailAlreadyExists e -> HttpStatus.CONFLICT;
                    case UserError.InvalidCredentials e -> HttpStatus.UNAUTHORIZED;
                    case UserError.InvalidRefreshToken e -> HttpStatus.UNAUTHORIZED;
                    case UserError.UserNotFound e -> HttpStatus.NOT_FOUND;
                    case UserError.UserAlreadyDeleted e -> HttpStatus.NOT_FOUND;
                    case UserError.UserHasActiveBookings e -> HttpStatus.CONFLICT;
                };
        ErrorCode code =
                switch (error) {
                    case UserError.EmailAlreadyExists e -> ErrorCode.USER_EMAIL_ALREADY_EXISTS;
                    case UserError.InvalidCredentials e -> ErrorCode.USER_INVALID_CREDENTIALS;
                    case UserError.InvalidRefreshToken e -> ErrorCode.USER_INVALID_REFRESH_TOKEN;
                    case UserError.UserNotFound e -> ErrorCode.USER_NOT_FOUND;
                    case UserError.UserAlreadyDeleted e -> ErrorCode.USER_NOT_FOUND;
                    case UserError.UserHasActiveBookings e -> ErrorCode.USER_HAS_ACTIVE_BOOKINGS;
                };
        return ResponseEntity.status(status)
                .body(JsendResponse.fail(new FailData(error.message(), code, List.of())));
    }
}
