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
import io.github.phunguy65.ttbs.backend.user.infrastructure.web.request.UpdateAuthenticatedUserRequest;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@DisplayName("AuthController update me endpoint")
class AuthControllerUpdateMeTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    private UpdateAuthenticatedUserUseCase updateAuthenticatedUserUseCase;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        updateAuthenticatedUserUseCase = mock(UpdateAuthenticatedUserUseCase.class);
        controller = new AuthController(
                mock(RegisterUserUseCase.class),
                mock(LoginUserUseCase.class),
                mock(RefreshTokenUseCase.class),
                mock(LogoutUserUseCase.class),
                mock(GetAuthenticatedUserUseCase.class),
                updateAuthenticatedUserUseCase,
                mock(DeleteAuthenticatedUserUseCase.class));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                new MockHttpServletRequest("PUT", "/api/v1.0/auth/me")));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Nested
    @DisplayName("black-box")
    class BlackBox {

        @Test
        @DisplayName("returns 200 and updated UserResponse for valid full update")
        void updateMe_returnsUpdatedUserResponseForValidFullUpdate() {
            when(updateAuthenticatedUserUseCase.execute(any()))
                    .thenReturn(Result.success(updatedUserResponse()));

            var result = controller.updateMe(authentication(USER_ID), validRequest());

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsendResponse<?> body = result.getBody();
            assertThat(body).isNotNull();
            assertThat(body.status()).isEqualTo("success");
            assertThat(body.data()).isEqualTo(updatedUserResponse());
        }

        @Test
        @DisplayName("returns 200 when updating with same email")
        void updateMe_returnsSuccessWhenUpdatingWithSameEmail() {
            when(updateAuthenticatedUserUseCase.execute(any()))
                    .thenReturn(Result.success(updatedUserResponse()));

            var result = controller.updateMe(authentication(USER_ID), validRequest());

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(((UserResponse) result.getBody().data()).email())
                    .isEqualTo("customer@example.com");
        }
    }

    @Nested
    @DisplayName("negative validation")
    class NegativeValidation {

        @Test
        @DisplayName("rejects blank fullName")
        void updateMe_rejectsBlankFullName() {
            var violations = validator.validate(new UpdateAuthenticatedUserRequest(
                    " ", "customer@example.com", "+84901234567", null, null, null, null));

            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("fullName"));
        }

        @Test
        @DisplayName("rejects blank email")
        void updateMe_rejectsBlankEmail() {
            var violations = validator.validate(new UpdateAuthenticatedUserRequest(
                    "Nguyen Van A", " ", "+84901234567", null, null, null, null));

            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        }

        @Test
        @DisplayName("rejects invalid email format")
        void updateMe_rejectsInvalidEmailFormat() {
            var violations = validator.validate(new UpdateAuthenticatedUserRequest(
                    "Nguyen Van A", "not-an-email", "+84901234567", null, null, null, null));

            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        }

        @Test
        @DisplayName("rejects blank-only phone")
        void updateMe_rejectsBlankOnlyPhone() {
            var violations = validator.validate(new UpdateAuthenticatedUserRequest(
                    "Nguyen Van A", "customer@example.com", " ", null, null, null, null));

            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("phone"));
        }

        @Test
        @DisplayName("rejects blank-only gender")
        void updateMe_rejectsBlankOnlyGender() {
            var violations = validator.validate(new UpdateAuthenticatedUserRequest(
                    "Nguyen Van A", "customer@example.com", null, null, " ", null, null));

            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("gender"));
        }

        @Test
        @DisplayName("rejects blank-only idDocumentNumber")
        void updateMe_rejectsBlankOnlyIdDocumentNumber() {
            var violations = validator.validate(new UpdateAuthenticatedUserRequest(
                    "Nguyen Van A", "customer@example.com", null, null, null, " ", null));

            assertThat(violations)
                    .anyMatch(v -> v.getPropertyPath().toString().equals("idDocumentNumber"));
        }

        @Test
        @DisplayName("rejects blank-only addressLine")
        void updateMe_rejectsBlankOnlyAddressLine() {
            var violations = validator.validate(new UpdateAuthenticatedUserRequest(
                    "Nguyen Van A", "customer@example.com", null, null, null, null, " "));

            assertThat(violations)
                    .anyMatch(v -> v.getPropertyPath().toString().equals("addressLine"));
        }

        @Test
        @DisplayName("accepts null optional fields")
        void updateMe_acceptsNullOptionalFields() {
            var violations = validator.validate(new UpdateAuthenticatedUserRequest(
                    "Nguyen Van A", "customer@example.com", null, null, null, null, null));

            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("negative business errors")
    class NegativeBusinessErrors {

        @Test
        @DisplayName("returns 404 when user not found")
        void updateMe_returnsUserNotFound() {
            when(updateAuthenticatedUserUseCase.execute(any()))
                    .thenReturn(Result.failure(new UserError.UserNotFound()));

            var result = controller.updateMe(authentication(USER_ID), validRequest());

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            JsonNode data = objectMapper.valueToTree(result.getBody().data());
            assertThat(data.get("code").asText()).isEqualTo("USER_NOT_FOUND");
        }

        @Test
        @DisplayName("returns 409 when email already taken")
        void updateMe_returnsEmailAlreadyExists() {
            when(updateAuthenticatedUserUseCase.execute(any()))
                    .thenReturn(Result.failure(new UserError.EmailAlreadyExists()));

            var result = controller.updateMe(authentication(USER_ID), validRequest());

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            JsonNode data = objectMapper.valueToTree(result.getBody().data());
            assertThat(data.get("code").asText()).isEqualTo("USER_EMAIL_ALREADY_EXISTS");
            assertThat(data.get("message").asText())
                    .isEqualTo("An account with this email already exists");
        }
    }

    @Nested
    @DisplayName("response contract")
    class ResponseContract {

        @Test
        @DisplayName("returns JSend success wrapper with updated user data")
        void updateMe_returnsJsendSuccessWrapperWithUpdatedUserData() {
            when(updateAuthenticatedUserUseCase.execute(any()))
                    .thenReturn(Result.success(updatedUserResponse()));

            var result = controller.updateMe(authentication(USER_ID), validRequest());

            JsonNode json = objectMapper.valueToTree(result.getBody());
            assertThat(json.get("status").asText()).isEqualTo("success");
            assertThat(json.get("data").get("id").asText()).isEqualTo(USER_ID.toString());
            assertThat(json.get("data").get("email").asText()).isEqualTo("customer@example.com");
            assertThat(json.get("data").get("fullName").asText()).isEqualTo("Nguyen Van B");
            assertThat(json.get("data").get("phone").asText()).isEqualTo("+84909998888");
            assertThat(json.get("data").get("role").asText()).isEqualTo("CUSTOMER");
        }
    }

    private Authentication authentication(UUID userId) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(userId.toString());
        return auth;
    }

    private UpdateAuthenticatedUserRequest validRequest() {
        return new UpdateAuthenticatedUserRequest(
                "Nguyen Van B",
                "customer@example.com",
                "+84909998888",
                LocalDate.of(1996, 6, 20),
                "female",
                "987654321000",
                "456 Updated Street");
    }

    private UserResponse updatedUserResponse() {
        return new UserResponse(
                USER_ID,
                "customer@example.com",
                "Nguyen Van B",
                "+84909998888",
                LocalDate.of(1996, 6, 20),
                "female",
                "987654321000",
                "456 Updated Street",
                "CUSTOMER",
                Instant.parse("2026-05-15T10:15:30Z"));
    }

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
}
