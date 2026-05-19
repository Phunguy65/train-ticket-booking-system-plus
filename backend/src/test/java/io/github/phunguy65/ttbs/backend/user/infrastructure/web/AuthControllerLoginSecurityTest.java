package io.github.phunguy65.ttbs.backend.user.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
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
import io.github.phunguy65.ttbs.backend.user.infrastructure.web.request.LoginRequest;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@DisplayName("AuthController login security")
class AuthControllerLoginSecurityTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    private LoginUserUseCase loginUserUseCase;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        loginUserUseCase = mock(LoginUserUseCase.class);
        controller = new AuthController(
                mock(RegisterUserUseCase.class),
                loginUserUseCase,
                mock(RefreshTokenUseCase.class),
                mock(LogoutUserUseCase.class),
                mock(GetAuthenticatedUserUseCase.class),
                mock(UpdateAuthenticatedUserUseCase.class),
                mock(DeleteAuthenticatedUserUseCase.class));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                new MockHttpServletRequest("POST", "/api/v1.0/auth/login")));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Nested
    @DisplayName("input handling")
    class InputHandling {

        @Test
        @DisplayName("rejects SQL injection payload in email with a validation error")
        void login_rejectsSqlInjectionEmail() {
            var violations =
                    validator.validate(new LoginRequest("'; DROP TABLE users; --", "secret123"));

            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        }

        @Test
        @DisplayName("rejects oversized email payloads without a server error")
        void login_rejectsOversizedEmailPayload() {
            String oversizedEmail = "a".repeat(10000) + "@example.com";

            var violations = validator.validate(new LoginRequest(oversizedEmail, "secret123"));

            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        }

        @Test
        @DisplayName("accepts XSS-like password as inert credential input")
        void login_acceptsXssPasswordAsCredentialInput() {
            when(loginUserUseCase.execute(any()))
                    .thenReturn(Result.failure(new UserError.InvalidCredentials()));

            var result = controller.login(
                    new LoginRequest("customer@example.com", "<script>alert('xss')</script>"));

            assertThat(validator.validate(new LoginRequest(
                            "customer@example.com", "<script>alert('xss')</script>")))
                    .isEmpty();
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("timing attack resistance")
    class TimingAttackResistance {

        @Test
        @DisplayName("returns identical failures for unknown email and wrong password")
        void login_returnsIdenticalFailuresForCredentialMismatchCases() {
            when(loginUserUseCase.execute(any()))
                    .thenReturn(Result.failure(new UserError.InvalidCredentials()));

            var unknownEmail =
                    controller.login(new LoginRequest("unknown@example.com", "secret123"));
            var wrongPassword = controller.login(new LoginRequest("customer@example.com", "wrong"));

            JsonNode unknownEmailData =
                    objectMapper.valueToTree(unknownEmail.getBody().data());
            JsonNode wrongPasswordData =
                    objectMapper.valueToTree(wrongPassword.getBody().data());
            assertThat(unknownEmailData.get("message").asText())
                    .isEqualTo("Invalid email or password");
            assertThat(wrongPasswordData.get("message").asText())
                    .isEqualTo("Invalid email or password");
            assertThat(unknownEmailData.get("code").asText()).isEqualTo("USER_INVALID_CREDENTIALS");
            assertThat(wrongPasswordData.get("code").asText())
                    .isEqualTo("USER_INVALID_CREDENTIALS");
            assertThat(unknownEmailData).isEqualTo(wrongPasswordData);
        }
    }

    @Nested
    @DisplayName("response hardening")
    class ResponseHardening {

        @Test
        @DisplayName("never exposes the password field in login responses")
        void login_doesNotExposePasswordField() {
            when(loginUserUseCase.execute(any())).thenReturn(Result.success(loginResponse()));

            var result = controller.login(new LoginRequest("safe@example.com", "secret123"));

            JsonNode json = objectMapper.valueToTree(result.getBody());
            assertThat(json.get("data").get("user").has("password")).isFalse();
        }

        @Test
        @DisplayName("never exposes the password hash field in login responses")
        void login_doesNotExposePasswordHashField() {
            when(loginUserUseCase.execute(any())).thenReturn(Result.success(loginResponse()));

            var result = controller.login(new LoginRequest("safe@example.com", "secret123"));

            JsonNode json = objectMapper.valueToTree(result.getBody());
            assertThat(json.get("data").get("user").has("passwordHash")).isFalse();
        }
    }

    @Test
    @DisplayName("declares @Valid on the login controller request body")
    void login_declaresValidOnRequestBody() throws Exception {
        Method loginMethod = AuthController.class.getDeclaredMethod("login", LoginRequest.class);

        assertThat(Arrays.stream(loginMethod.getParameterAnnotations()[0])
                        .map(annotation -> annotation.annotationType().getName())
                        .toList())
                .contains(Valid.class.getName());
    }

    private LoginResultResponse loginResponse() {
        return new LoginResultResponse(
                "access-token",
                "refresh-token",
                new UserResponse(
                        UUID.fromString("33333333-3333-3333-3333-333333333333"),
                        "safe@example.com",
                        "Safe User",
                        null,
                        null,
                        null,
                        null,
                        null,
                        "CUSTOMER",
                        Instant.parse("2026-05-15T12:00:00Z")));
    }
}
