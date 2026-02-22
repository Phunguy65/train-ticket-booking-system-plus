package io.github.phunguy65.ttbs.backend.user.infrastructure.web;

import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.ErrorCode;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.FailData;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JsendResponse;
import io.github.phunguy65.ttbs.backend.user.application.usecase.LoginUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.LogoutUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.RefreshTokenUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.RegisterUserUseCase;
import io.github.phunguy65.ttbs.backend.user.domain.errors.UserError;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUserUseCase loginUserUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUserUseCase logoutUserUseCase;
    private final AuthRequestMapper mapper;

    AuthController(
            RegisterUserUseCase registerUserUseCase,
            LoginUserUseCase loginUserUseCase,
            RefreshTokenUseCase refreshTokenUseCase,
            LogoutUserUseCase logoutUserUseCase,
            AuthRequestMapper mapper) {
        this.registerUserUseCase = registerUserUseCase;
        this.loginUserUseCase = loginUserUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUserUseCase = logoutUserUseCase;
        this.mapper = mapper;
    }

    @PostMapping("/register")
    ResponseEntity<JsendResponse<?>> register(@Valid @RequestBody RegisterHttpRequest request) {
        return registerUserUseCase
                .execute(mapper.toCommand(request))
                .fold(
                        userDto -> ResponseEntity.created(URI.create("/api/v1/auth/register"))
                                .body(JsendResponse.success(mapper.toResponse(userDto))),
                        error -> errorResponse(error));
    }

    @PostMapping("/login")
    ResponseEntity<JsendResponse<?>> login(@Valid @RequestBody LoginHttpRequest request) {
        return loginUserUseCase
                .execute(mapper.toCommand(request))
                .fold(
                        loginResult -> ResponseEntity.ok(
                                JsendResponse.success(mapper.toLoginResponse(loginResult))),
                        error -> errorResponse(error));
    }

    @PostMapping("/refresh")
    ResponseEntity<JsendResponse<?>> refresh(@Valid @RequestBody RefreshTokenHttpRequest request) {
        return refreshTokenUseCase
                .execute(mapper.toCommand(request))
                .fold(
                        loginResult -> ResponseEntity.ok(
                                JsendResponse.success(mapper.toLoginResponse(loginResult))),
                        error -> errorResponse(error));
    }

    @PostMapping("/logout")
    ResponseEntity<JsendResponse<?>> logout(@Valid @RequestBody RefreshTokenHttpRequest request) {
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
                };
        ErrorCode code =
                switch (error) {
                    case UserError.EmailAlreadyExists e -> ErrorCode.USER_EMAIL_ALREADY_EXISTS;
                    case UserError.InvalidCredentials e -> ErrorCode.USER_INVALID_CREDENTIALS;
                    case UserError.InvalidRefreshToken e -> ErrorCode.USER_INVALID_REFRESH_TOKEN;
                    case UserError.UserNotFound e -> ErrorCode.USER_NOT_FOUND;
                };
        return ResponseEntity.status(status)
                .body(JsendResponse.fail(new FailData(error.message(), code, List.of())));
    }
}
