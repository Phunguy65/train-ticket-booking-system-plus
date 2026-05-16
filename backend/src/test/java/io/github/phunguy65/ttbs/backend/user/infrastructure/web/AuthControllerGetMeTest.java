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

@DisplayName("AuthController me endpoint")
class AuthControllerGetMeTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private GetAuthenticatedUserUseCase getAuthenticatedUserUseCase;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        getAuthenticatedUserUseCase = mock(GetAuthenticatedUserUseCase.class);
        controller = new AuthController(
                mock(RegisterUserUseCase.class),
                mock(LoginUserUseCase.class),
                mock(RefreshTokenUseCase.class),
                mock(LogoutUserUseCase.class),
                getAuthenticatedUserUseCase,
                mock(UpdateAuthenticatedUserUseCase.class),
                mock(DeleteAuthenticatedUserUseCase.class));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                new MockHttpServletRequest("GET", "/api/v1.0/auth/me")));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Nested
    @DisplayName("black-box")
    class BlackBox {

        @Test
        @DisplayName("returns 200 and full UserResponse for authenticated user")
        void me_returnsFullUserResponseForAuthenticatedUser() {
            when(getAuthenticatedUserUseCase.execute(any()))
                    .thenReturn(Result.success(fullUserResponse()));

            var result = controller.me(authentication(PRIMARY_USER_ID));

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsendResponse<?> body = result.getBody();
            assertThat(body).isNotNull();
            assertThat(body.status()).isEqualTo("success");
            assertThat(body.message()).isNull();
            assertThat(body.data()).isEqualTo(fullUserResponse());
        }

        @Test
        @DisplayName("returns 200 with nullable fields as null")
        void me_returnsUserResponseWithNullableFieldsAsNull() {
            UserResponse response = nullableUserResponse();
            when(getAuthenticatedUserUseCase.execute(any())).thenReturn(Result.success(response));

            var result = controller.me(authentication(PRIMARY_USER_ID));

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            UserResponse body = (UserResponse) result.getBody().data();
            assertThat(body.phone()).isNull();
            assertThat(body.dateOfBirth()).isNull();
            assertThat(body.gender()).isNull();
            assertThat(body.idDocumentNumber()).isNull();
            assertThat(body.addressLine()).isNull();
        }

        @Test
        @DisplayName("returns 404 with USER_NOT_FOUND when user does not exist")
        void me_returnsUserNotFoundWhenUserDoesNotExist() {
            when(getAuthenticatedUserUseCase.execute(any()))
                    .thenReturn(Result.failure(new UserError.UserNotFound()));

            var result = controller.me(authentication(PRIMARY_USER_ID));

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            JsonNode data = objectMapper.valueToTree(result.getBody().data());
            assertThat(data.get("message").asText()).isEqualTo("User not found");
            assertThat(data.get("code").asText()).isEqualTo("USER_NOT_FOUND");
            assertThat(data.get("errors")).isEmpty();
        }
    }

    @Nested
    @DisplayName("response contract")
    class ResponseContract {

        @Test
        @DisplayName("returns JSend success wrapper with data containing all user fields")
        void me_returnsJsendSuccessWrapperWithAllUserFields() {
            when(getAuthenticatedUserUseCase.execute(any()))
                    .thenReturn(Result.success(fullUserResponse()));

            var result = controller.me(authentication(PRIMARY_USER_ID));

            JsonNode json = objectMapper.valueToTree(result.getBody());
            assertThat(json.get("status").asText()).isEqualTo("success");
            assertThat(json.get("data").get("id").asText()).isEqualTo(PRIMARY_USER_ID.toString());
            assertThat(json.get("data").get("email").asText()).isEqualTo("customer@example.com");
            assertThat(json.get("data").get("fullName").asText()).isEqualTo("Nguyen Van A");
            assertThat(json.get("data").get("phone").asText()).isEqualTo("+84901234567");
            assertThat(json.get("data").get("role").asText()).isEqualTo("CUSTOMER");
            assertThat(json.get("data").has("createdAt")).isTrue();
        }

        @Test
        @DisplayName("returns JSend fail wrapper for error responses")
        void me_returnsJsendFailWrapperForErrorResponses() {
            when(getAuthenticatedUserUseCase.execute(any()))
                    .thenReturn(Result.failure(new UserError.UserNotFound()));

            var result = controller.me(authentication(PRIMARY_USER_ID));

            JsonNode json = objectMapper.valueToTree(result.getBody());
            assertThat(json.get("status").asText()).isEqualTo("fail");
            assertThat(json.get("data").get("message").asText()).isEqualTo("User not found");
            assertThat(json.get("data").get("code").asText()).isEqualTo("USER_NOT_FOUND");
        }
    }

    private Authentication authentication(UUID userId) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(userId.toString());
        return auth;
    }

    private UserResponse fullUserResponse() {
        return new UserResponse(
                PRIMARY_USER_ID,
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

    private UserResponse nullableUserResponse() {
        return new UserResponse(
                PRIMARY_USER_ID,
                "nullable@example.com",
                "Nullable User",
                null,
                null,
                null,
                null,
                null,
                "CUSTOMER",
                Instant.parse("2026-05-15T10:15:30Z"));
    }

    private static final UUID PRIMARY_USER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
}
