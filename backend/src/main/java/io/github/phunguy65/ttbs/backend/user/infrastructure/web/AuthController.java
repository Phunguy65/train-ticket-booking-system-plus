package io.github.phunguy65.ttbs.backend.user.infrastructure.web;

import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.ErrorCode;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.FailData;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JsendResponse;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.SuccessPayload;
import io.github.phunguy65.ttbs.backend.user.application.command.SoftDeleteUserCommand;
import io.github.phunguy65.ttbs.backend.user.application.query.GetUserByIdQuery;
import io.github.phunguy65.ttbs.backend.user.application.response.LoginResultResponse;
import io.github.phunguy65.ttbs.backend.user.application.response.UserResponse;
import io.github.phunguy65.ttbs.backend.user.application.usecase.DeleteAuthenticatedUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.GetAuthenticatedUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.LoginUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.LogoutUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.RefreshTokenUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.RegisterUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.UpdateAuthenticatedUserUseCase;
import io.github.phunguy65.ttbs.backend.user.domain.error.UserError;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import io.github.phunguy65.ttbs.backend.user.infrastructure.web.request.LoginRequest;
import io.github.phunguy65.ttbs.backend.user.infrastructure.web.request.RefreshTokenRequest;
import io.github.phunguy65.ttbs.backend.user.infrastructure.web.request.RegisterRequest;
import io.github.phunguy65.ttbs.backend.user.infrastructure.web.request.UpdateAuthenticatedUserRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/{version}/auth")
@Tag(name = "Authentication")
class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUserUseCase loginUserUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUserUseCase logoutUserUseCase;
    private final GetAuthenticatedUserUseCase getAuthenticatedUserUseCase;
    private final UpdateAuthenticatedUserUseCase updateAuthenticatedUserUseCase;
    private final DeleteAuthenticatedUserUseCase deleteAuthenticatedUserUseCase;

    AuthController(
            RegisterUserUseCase registerUserUseCase,
            LoginUserUseCase loginUserUseCase,
            RefreshTokenUseCase refreshTokenUseCase,
            LogoutUserUseCase logoutUserUseCase,
            GetAuthenticatedUserUseCase getAuthenticatedUserUseCase,
            UpdateAuthenticatedUserUseCase updateAuthenticatedUserUseCase,
            DeleteAuthenticatedUserUseCase deleteAuthenticatedUserUseCase) {
        this.registerUserUseCase = registerUserUseCase;
        this.loginUserUseCase = loginUserUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUserUseCase = logoutUserUseCase;
        this.getAuthenticatedUserUseCase = getAuthenticatedUserUseCase;
        this.updateAuthenticatedUserUseCase = updateAuthenticatedUserUseCase;
        this.deleteAuthenticatedUserUseCase = deleteAuthenticatedUserUseCase;
    }

    @Operation(operationId = "register", summary = "Register a customer account")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Customer account created"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid registration payload",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse"))),
        @ApiResponse(
                responseCode = "409",
                description = "Email address already exists",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse")))
    })
    @SecurityRequirement(name = "")
    @SuccessPayload(value = UserResponse.class, responseCode = "201")
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

    @Operation(operationId = "login", summary = "Authenticate a customer")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Authentication successful"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid login payload",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse"))),
        @ApiResponse(
                responseCode = "401",
                description = "Invalid customer credentials",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse")))
    })
    @SecurityRequirement(name = "")
    @SuccessPayload(LoginResultResponse.class)
    @PostMapping(value = "/login", version = "1.0")
    ResponseEntity<JsendResponse<?>> login(@Valid @RequestBody LoginRequest request) {
        return loginUserUseCase
                .execute(request.toCommand())
                .fold(
                        loginResult -> ResponseEntity.ok(JsendResponse.success(loginResult)),
                        error -> errorResponse(error));
    }

    @Operation(operationId = "refreshToken", summary = "Rotate access and refresh tokens")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token pair rotated"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid refresh-token payload",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse"))),
        @ApiResponse(
                responseCode = "401",
                description = "Refresh token is invalid or expired",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse")))
    })
    @SecurityRequirement(name = "")
    @SuccessPayload(LoginResultResponse.class)
    @PostMapping(value = "/refresh", version = "1.0")
    ResponseEntity<JsendResponse<?>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return refreshTokenUseCase
                .execute(request.toCommand())
                .fold(
                        loginResult -> ResponseEntity.ok(JsendResponse.success(loginResult)),
                        error -> errorResponse(error));
    }

    @Operation(operationId = "logout", summary = "Revoke the current refresh token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Refresh token revoked"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid refresh-token payload",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse"))),
        @ApiResponse(
                responseCode = "401",
                description = "Refresh token is invalid or expired",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse")))
    })
    @SecurityRequirement(name = "")
    @SuccessPayload
    @PostMapping(value = "/logout", version = "1.0")
    ResponseEntity<JsendResponse<?>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        return logoutUserUseCase
                .execute(request.toLogoutCommand())
                .fold(
                        ignored -> ResponseEntity.ok(JsendResponse.success()),
                        error -> errorResponse(error));
    }

    @Operation(operationId = "getAuthenticatedUser", summary = "Get the current customer profile")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Authenticated customer profile"),
        @ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse"))),
        @ApiResponse(
                responseCode = "404",
                description = "Customer profile not found",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse")))
    })
    @SecurityRequirement(name = "bearerAuth")
    @SuccessPayload(UserResponse.class)
    @GetMapping(value = "/me", version = "1.0")
    @PreAuthorize("isAuthenticated()")
    ResponseEntity<JsendResponse<?>> me(@Parameter(hidden = true) Authentication auth) {
        UUID principalId = UUID.fromString(auth.getName());
        return getAuthenticatedUserUseCase
                .execute(new GetUserByIdQuery(principalId))
                .fold(
                        userDto -> ResponseEntity.ok(JsendResponse.success(userDto)),
                        error -> errorResponse(error));
    }

    @Operation(
            operationId = "updateAuthenticatedUser",
            summary = "Replace the current customer profile",
            description =
                    "Performs a full replacement of the authenticated customer's editable profile fields.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Customer profile updated"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid update payload",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse"))),
        @ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse"))),
        @ApiResponse(
                responseCode = "404",
                description = "Customer profile not found",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse"))),
        @ApiResponse(
                responseCode = "409",
                description = "Email address already exists",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse")))
    })
    @SecurityRequirement(name = "bearerAuth")
    @SuccessPayload(UserResponse.class)
    @PutMapping(value = "/me", version = "1.0")
    @PreAuthorize("isAuthenticated()")
    ResponseEntity<JsendResponse<?>> updateMe(
            @Parameter(hidden = true) Authentication auth,
            @Valid @RequestBody UpdateAuthenticatedUserRequest request) {
        UUID principalId = UUID.fromString(auth.getName());
        return updateAuthenticatedUserUseCase
                .execute(request.toCommand(principalId))
                .fold(
                        userDto -> ResponseEntity.ok(JsendResponse.success(userDto)),
                        error -> errorResponse(error));
    }

    @Operation(
            operationId = "deleteAuthenticatedUser",
            summary = "Soft-delete the current customer account")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Customer account deleted"),
        @ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse"))),
        @ApiResponse(
                responseCode = "404",
                description = "Customer profile not found",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse"))),
        @ApiResponse(
                responseCode = "409",
                description = "Customer still has active bookings",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse")))
    })
    @SecurityRequirement(name = "bearerAuth")
    @SuccessPayload
    @DeleteMapping(value = "/me", version = "1.0")
    @PreAuthorize("isAuthenticated()")
    ResponseEntity<JsendResponse<?>> deleteMe(@Parameter(hidden = true) Authentication auth) {
        UUID principalId = UUID.fromString(auth.getName());
        return deleteAuthenticatedUserUseCase
                .execute(new SoftDeleteUserCommand(UserId.of(principalId)))
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
