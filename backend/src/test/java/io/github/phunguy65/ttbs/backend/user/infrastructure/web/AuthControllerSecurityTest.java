package io.github.phunguy65.ttbs.backend.user.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.user.application.response.UserResponse;
import io.github.phunguy65.ttbs.backend.user.application.usecase.DeleteAuthenticatedUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.GetAuthenticatedUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.LoginUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.LogoutUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.RefreshTokenUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.RegisterUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.UpdateAuthenticatedUserUseCase;
import io.github.phunguy65.ttbs.backend.user.infrastructure.web.request.RegisterRequest;
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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@DisplayName("AuthController register security")
class AuthControllerSecurityTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    private RegisterUserUseCase registerUserUseCase;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        registerUserUseCase = mock(RegisterUserUseCase.class);
        controller = new AuthController(
                registerUserUseCase,
                mock(LoginUserUseCase.class),
                mock(RefreshTokenUseCase.class),
                mock(LogoutUserUseCase.class),
                mock(GetAuthenticatedUserUseCase.class),
                mock(UpdateAuthenticatedUserUseCase.class),
                mock(DeleteAuthenticatedUserUseCase.class));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                new MockHttpServletRequest("POST", "/api/v1.0/auth/register")));
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
        void register_rejectsSqlInjectionEmail() {
            var violations = validator.validate(
                    new RegisterRequest("'; DROP TABLE users; --", "secret123", "Nguyen Van A"));

            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        }

        @Test
        @DisplayName("accepts XSS-like full name as inert string data in the response")
        void register_returnsXssPayloadAsDataOnly() {
            String xssFullName = "<script>alert('xss')</script>";
            when(registerUserUseCase.execute(any()))
                    .thenReturn(Result.success(userResponse("xss@example.com", xssFullName)));

            var result = controller.register(
                    new RegisterRequest("xss@example.com", "secret123", xssFullName));

            JsonNode json = objectMapper.valueToTree(result.getBody());
            assertThat(json.get("status").asText()).isEqualTo("success");
            assertThat(json.get("data").get("fullName").asText()).isEqualTo(xssFullName);
            assertThat(json.get("data").has("password")).isFalse();
            assertThat(json.get("data").has("passwordHash")).isFalse();
        }

        @Test
        @DisplayName("rejects oversized email payloads without a server error")
        void register_rejectsOversizedEmailPayload() {
            String oversizedEmail = "a".repeat(10000) + "@example.com";

            var violations = validator.validate(
                    new RegisterRequest(oversizedEmail, "secret123", "Nguyen Van A"));

            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        }
    }

    @Nested
    @DisplayName("response hardening")
    class ResponseHardening {

        @Test
        @DisplayName("never exposes the password field in registration responses")
        void register_doesNotExposePasswordField() {
            when(registerUserUseCase.execute(any()))
                    .thenReturn(Result.success(userResponse("safe@example.com", "Safe User")));

            var result = controller.register(
                    new RegisterRequest("safe@example.com", "secret123", "Safe User"));

            JsonNode json = objectMapper.valueToTree(result.getBody());
            assertThat(json.get("data").has("password")).isFalse();
        }

        @Test
        @DisplayName("never exposes the password hash field in registration responses")
        void register_doesNotExposePasswordHashField() {
            when(registerUserUseCase.execute(any()))
                    .thenReturn(Result.success(
                            userResponse("hash-safe@example.com", "Hash Safe User")));

            var result = controller.register(
                    new RegisterRequest("hash-safe@example.com", "secret123", "Hash Safe User"));

            JsonNode json = objectMapper.valueToTree(result.getBody());
            assertThat(json.get("data").has("passwordHash")).isFalse();
        }
    }

    @Test
    @DisplayName("declares @Valid on the register controller request body")
    void register_declaresValidOnRequestBody() throws Exception {
        Method registerMethod =
                AuthController.class.getDeclaredMethod("register", RegisterRequest.class);

        assertThat(Arrays.stream(registerMethod.getParameterAnnotations()[0])
                        .map(annotation -> annotation.annotationType().getName())
                        .toList())
                .contains(Valid.class.getName());
    }

    private UserResponse userResponse(String email, String fullName) {
        return new UserResponse(
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                email,
                fullName,
                null,
                null,
                null,
                null,
                null,
                "CUSTOMER",
                Instant.parse("2026-05-15T12:00:00Z"));
    }
}
