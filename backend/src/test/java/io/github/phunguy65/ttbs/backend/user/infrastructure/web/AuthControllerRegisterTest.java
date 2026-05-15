package io.github.phunguy65.ttbs.backend.user.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JsendResponse;
import io.github.phunguy65.ttbs.backend.user.application.response.UserResponse;
import io.github.phunguy65.ttbs.backend.user.application.usecase.DeleteAuthenticatedUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.GetAuthenticatedUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.LoginUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.LogoutUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.RefreshTokenUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.RegisterUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.UpdateAuthenticatedUserUseCase;
import io.github.phunguy65.ttbs.backend.user.domain.error.UserError;
import io.github.phunguy65.ttbs.backend.user.infrastructure.web.request.RegisterRequest;
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

@DisplayName("AuthController register endpoint")
class AuthControllerRegisterTest {

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
    @DisplayName("black-box")
    class BlackBox {

        @Test
        @DisplayName("returns 201 and a JSend user payload for a valid registration")
        void register_returnsCreatedUserResponse() {
            UserResponse response = completeUserResponse();
            when(registerUserUseCase.execute(any())).thenReturn(Result.success(response));

            var result = controller.register(
                    new RegisterRequest("customer@example.com", "secret123", "Nguyen Van A"));

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getHeaders().getLocation()).isNotNull();
            JsendResponse<?> body = result.getBody();
            assertThat(body).isNotNull();
            assertThat(body.status()).isEqualTo("success");
            assertThat(body.message()).isNull();
            assertThat(body.data()).isEqualTo(response);
            UserResponse data = (UserResponse) body.data();
            assertThat(data.id())
                    .isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));
            assertThat(data.email()).isEqualTo("customer@example.com");
            assertThat(data.fullName()).isEqualTo("Nguyen Van A");
            assertThat(data.phone()).isEqualTo("+84901234567");
            assertThat(data.dateOfBirth()).isEqualTo(LocalDate.of(1995, 5, 15));
            assertThat(data.gender()).isEqualTo("female");
            assertThat(data.idDocumentNumber()).isEqualTo("012345678901");
            assertThat(data.addressLine()).isEqualTo("123 Test Street");
            assertThat(data.role()).isEqualTo("CUSTOMER");
            assertThat(data.createdAt()).isEqualTo(Instant.parse("2026-05-15T10:15:30Z"));
        }
    }

    @Nested
    @DisplayName("negative")
    class Negative {

        @Test
        @DisplayName("detects invalid email format as a validation error")
        void register_detectsInvalidEmailFormat() {
            var violations = validator.validate(
                    new RegisterRequest("invalid-email", "secret123", "Nguyen Van A"));

            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        }

        @Test
        @DisplayName("detects missing required fields as validation errors")
        void register_detectsMissingRequiredFields() {
            var violations = validator.validate(new RegisterRequest(null, null, null));

            assertThat(violations).hasSize(3);
        }

        @Test
        @DisplayName("detects passwords shorter than 8 characters")
        void register_detectsShortPassword() {
            var violations = validator.validate(
                    new RegisterRequest("customer@example.com", "short", "Nguyen Van A"));

            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
        }

        @Test
        @DisplayName("returns 409 when the email already exists")
        void register_returnsConflictForDuplicateEmail() {
            when(registerUserUseCase.execute(any()))
                    .thenReturn(Result.failure(new UserError.EmailAlreadyExists()));

            var result = controller.register(
                    new RegisterRequest("customer@example.com", "secret123", "Nguyen Van A"));

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            JsendResponse<?> body = result.getBody();
            assertThat(body).isNotNull();
            assertThat(body.status()).isEqualTo("fail");
            JsonNode data = objectMapper.valueToTree(body.data());
            assertThat(data.get("code").asText()).isEqualTo("USER_EMAIL_ALREADY_EXISTS");
            assertThat(data.get("message").asText())
                    .isEqualTo("An account with this email already exists");
            assertThat(data.get("errors")).isEmpty();
        }
    }

    @Nested
    @DisplayName("response contract")
    class ResponseContract {

        @Test
        @DisplayName("returns the expected JSend response body structure")
        void register_returnsExpectedJsendStructure() {
            when(registerUserUseCase.execute(any()))
                    .thenReturn(Result.success(completeUserResponse()));

            var result = controller.register(
                    new RegisterRequest("customer@example.com", "secret123", "Nguyen Van A"));

            JsonNode json = objectMapper.valueToTree(result.getBody());
            assertThat(json.get("status").asText()).isEqualTo("success");
            assertThat(json.has("data")).isTrue();
            assertThat(json.get("data").has("id")).isTrue();
            assertThat(json.get("data").has("email")).isTrue();
            assertThat(json.get("data").has("fullName")).isTrue();
            assertThat(json.get("data").has("role")).isTrue();
            assertThat(json.get("data").has("createdAt")).isTrue();
            assertThat(json.get("data").has("password")).isFalse();
            assertThat(json.get("data").has("passwordHash")).isFalse();
        }
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
