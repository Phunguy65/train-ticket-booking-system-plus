package io.github.phunguy65.ttbs.backend.user.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JsendResponse;
import io.github.phunguy65.ttbs.backend.user.application.usecase.DeleteAuthenticatedUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.GetAuthenticatedUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.LoginUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.LogoutUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.RefreshTokenUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.RegisterUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.UpdateAuthenticatedUserUseCase;
import io.github.phunguy65.ttbs.backend.user.domain.error.UserError;
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

@DisplayName("AuthController deleteMe endpoint")
class AuthControllerDeleteMeTest {

    private static final UUID USER_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private DeleteAuthenticatedUserUseCase deleteAuthenticatedUserUseCase;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        deleteAuthenticatedUserUseCase = mock(DeleteAuthenticatedUserUseCase.class);
        controller = new AuthController(
                mock(RegisterUserUseCase.class),
                mock(LoginUserUseCase.class),
                mock(RefreshTokenUseCase.class),
                mock(LogoutUserUseCase.class),
                mock(GetAuthenticatedUserUseCase.class),
                mock(UpdateAuthenticatedUserUseCase.class),
                deleteAuthenticatedUserUseCase);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                new MockHttpServletRequest("DELETE", "/api/v1.0/auth/me")));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Nested
    @DisplayName("black-box")
    class BlackBox {

        @Test
        @DisplayName("returns 200 with no data for successful deletion")
        void deleteMe_returnsSuccessWithoutDataForSuccessfulDeletion() {
            when(deleteAuthenticatedUserUseCase.execute(any())).thenReturn(Result.success());

            var result = controller.deleteMe(authentication(USER_ID));

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsendResponse<?> body = result.getBody();
            assertThat(body).isNotNull();
            assertThat(body.status()).isEqualTo("success");
            assertThat(body.data()).isNull();
        }

        @Test
        @DisplayName("returns 200 for idempotent deletion")
        void deleteMe_returnsSuccessForIdempotentDeletion() {
            when(deleteAuthenticatedUserUseCase.execute(any())).thenReturn(Result.success());

            var result = controller.deleteMe(authentication(USER_ID));

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().status()).isEqualTo("success");
            assertThat(result.getBody().data()).isNull();
        }
    }

    @Nested
    @DisplayName("negative business errors")
    class NegativeBusinessErrors {

        @Test
        @DisplayName("returns 404 with USER_NOT_FOUND when user does not exist")
        void deleteMe_returnsUserNotFoundWhenUserDoesNotExist() {
            when(deleteAuthenticatedUserUseCase.execute(any()))
                    .thenReturn(Result.failure(new UserError.UserNotFound()));

            var result = controller.deleteMe(authentication(USER_ID));

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            JsonNode data = objectMapper.valueToTree(result.getBody().data());
            assertThat(data.get("code").asText()).isEqualTo("USER_NOT_FOUND");
            assertThat(data.get("message").asText()).isEqualTo("User not found");
        }

        @Test
        @DisplayName("returns 409 with USER_HAS_ACTIVE_BOOKINGS when user has active bookings")
        void deleteMe_returnsUserHasActiveBookingsWhenUserHasActiveBookings() {
            when(deleteAuthenticatedUserUseCase.execute(any()))
                    .thenReturn(Result.failure(new UserError.UserHasActiveBookings()));

            var result = controller.deleteMe(authentication(USER_ID));

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            JsonNode data = objectMapper.valueToTree(result.getBody().data());
            assertThat(data.get("code").asText()).isEqualTo("USER_HAS_ACTIVE_BOOKINGS");
            assertThat(data.get("message").asText())
                    .isEqualTo("User has active bookings and cannot be deleted");
        }
    }

    @Nested
    @DisplayName("response contract")
    class ResponseContract {

        @Test
        @DisplayName("returns JSend success wrapper without data field for successful deletion")
        void deleteMe_returnsJsendSuccessWrapperWithoutDataFieldForSuccessfulDeletion() {
            when(deleteAuthenticatedUserUseCase.execute(any())).thenReturn(Result.success());

            var result = controller.deleteMe(authentication(USER_ID));

            JsonNode json = objectMapper.valueToTree(result.getBody());
            assertThat(json.get("status").asText()).isEqualTo("success");
            assertThat(json.has("data")).isFalse();
        }

        @Test
        @DisplayName("returns JSend fail wrapper for error responses")
        void deleteMe_returnsJsendFailWrapperForErrorResponses() {
            when(deleteAuthenticatedUserUseCase.execute(any()))
                    .thenReturn(Result.failure(new UserError.UserNotFound()));

            var result = controller.deleteMe(authentication(USER_ID));

            JsonNode json = objectMapper.valueToTree(result.getBody());
            assertThat(json.get("status").asText()).isEqualTo("fail");
            assertThat(json.get("data").get("message").asText()).isEqualTo("User not found");
            assertThat(json.get("data").get("code").asText()).isEqualTo("USER_NOT_FOUND");
            assertThat(json.get("data").get("errors")).isEmpty();
        }
    }

    private Authentication authentication(UUID userId) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(userId.toString());
        return auth;
    }
}
