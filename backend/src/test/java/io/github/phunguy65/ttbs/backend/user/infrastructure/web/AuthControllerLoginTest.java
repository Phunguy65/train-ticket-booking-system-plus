package io.github.phunguy65.ttbs.backend.user.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JsendResponse;
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
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.Instant;
import java.time.LocalDate;
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

@DisplayName("AuthController login endpoint")
class AuthControllerLoginTest {

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
    @DisplayName("black-box")
    class BlackBox {

        @Test
        @DisplayName("returns 200 and a JSend login payload for valid credentials")
        void login_returnsSuccessfulLoginResponse() {
            LoginResultResponse response = completeLoginResponse();
            when(loginUserUseCase.execute(any())).thenReturn(Result.success(response));

            var result = controller.login(new LoginRequest("customer@example.com", "secret123"));

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsendResponse<?> body = result.getBody();
            assertThat(body).isNotNull();
            assertThat(body.status()).isEqualTo("success");
            assertThat(body.message()).isNull();
            assertThat(body.data()).isEqualTo(response);
            LoginResultResponse data = (LoginResultResponse) body.data();
            assertThat(data.accessToken()).isEqualTo("access-token");
            assertThat(data.refreshToken()).isEqualTo("refresh-token");
            assertThat(data.user().id())
                    .isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));
            assertThat(data.user().email()).isEqualTo("customer@example.com");
            assertThat(data.user().fullName()).isEqualTo("Nguyen Van A");
            assertThat(data.user().phone()).isEqualTo("+84901234567");
            assertThat(data.user().dateOfBirth()).isEqualTo(LocalDate.of(1995, 5, 15));
            assertThat(data.user().gender()).isEqualTo("female");
            assertThat(data.user().idDocumentNumber()).isEqualTo("012345678901");
            assertThat(data.user().addressLine()).isEqualTo("123 Test Street");
            assertThat(data.user().role()).isEqualTo("CUSTOMER");
            assertThat(data.user().createdAt()).isEqualTo(Instant.parse("2026-05-15T10:15:30Z"));
        }
    }

    @Nested
    @DisplayName("negative")
    class Negative {

        @Test
        @DisplayName("detects invalid email format as a validation error")
        void login_detectsInvalidEmailFormat() {
            var violations = validator.validate(new LoginRequest("invalid-email", "secret123"));

            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        }

        @Test
        @DisplayName("detects missing required fields as validation errors")
        void login_detectsMissingRequiredFields() {
            var violations = validator.validate(new LoginRequest(null, null));

            assertThat(violations).hasSize(2);
        }

        @Test
        @DisplayName("returns 401 for invalid credentials")
        void login_returnsUnauthorizedForInvalidCredentials() {
            when(loginUserUseCase.execute(any()))
                    .thenReturn(Result.failure(new UserError.InvalidCredentials()));

            var result =
                    controller.login(new LoginRequest("customer@example.com", "wrong-password"));

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            JsendResponse<?> body = result.getBody();
            assertThat(body).isNotNull();
            assertThat(body.status()).isEqualTo("fail");
            JsonNode data = objectMapper.valueToTree(body.data());
            assertThat(data.get("code").asText()).isEqualTo("USER_INVALID_CREDENTIALS");
            assertThat(data.get("message").asText()).isEqualTo("Invalid email or password");
            assertThat(data.get("errors")).isEmpty();
        }
    }

    @Nested
    @DisplayName("response contract")
    class ResponseContract {

        @Test
        @DisplayName("returns the expected JSend response body structure")
        void login_returnsExpectedJsendStructure() {
            when(loginUserUseCase.execute(any()))
                    .thenReturn(Result.success(completeLoginResponse()));

            var result = controller.login(new LoginRequest("customer@example.com", "secret123"));

            JsonNode json = objectMapper.valueToTree(result.getBody());
            assertThat(json.get("status").asText()).isEqualTo("success");
            assertThat(json.has("data")).isTrue();
            assertThat(json.get("data").has("accessToken")).isTrue();
            assertThat(json.get("data").has("refreshToken")).isTrue();
            assertThat(json.get("data").has("user")).isTrue();
            assertThat(json.get("data").get("user").has("password")).isFalse();
            assertThat(json.get("data").get("user").has("passwordHash")).isFalse();
        }
    }

    private LoginResultResponse completeLoginResponse() {
        return new LoginResultResponse("access-token", "refresh-token", completeUserResponse());
    }

    private UserResponse completeUserResponse() {
        return new UserResponse(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "customer@example.com",
                "Nguyen Van A",
                "+84901234567",
                LocalDate.of(1995, 5, 15),
                "female",
                "012345678901",
                "123 Test Street",
                "CUSTOMER",
                Instant.parse("2026-05-15T10:15:30Z"));
    }
}
