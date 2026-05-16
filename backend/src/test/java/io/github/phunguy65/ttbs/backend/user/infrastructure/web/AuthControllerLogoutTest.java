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
import io.github.phunguy65.ttbs.backend.user.infrastructure.web.request.RefreshTokenRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@DisplayName("AuthController logout endpoint")
class AuthControllerLogoutTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    private LogoutUserUseCase logoutUserUseCase;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        logoutUserUseCase = mock(LogoutUserUseCase.class);
        controller = new AuthController(
                mock(RegisterUserUseCase.class),
                mock(LoginUserUseCase.class),
                mock(RefreshTokenUseCase.class),
                logoutUserUseCase,
                mock(GetAuthenticatedUserUseCase.class),
                mock(UpdateAuthenticatedUserUseCase.class),
                mock(DeleteAuthenticatedUserUseCase.class));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                new MockHttpServletRequest("POST", "/api/v1.0/auth/logout")));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Nested
    @DisplayName("black-box")
    class BlackBox {

        @Test
        @DisplayName("returns 200 and JSend success for a valid refresh token")
        void logout_returnsSuccessForValidRefreshToken() {
            when(logoutUserUseCase.execute(any())).thenReturn(Result.success());

            var result = controller.logout(new RefreshTokenRequest("refresh-token"));

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsendResponse<?> body = result.getBody();
            assertThat(body).isNotNull();
            assertThat(body.status()).isEqualTo("success");
            assertThat(body.message()).isNull();
            assertThat(body.data()).isNull();
        }

        @Test
        @DisplayName("returns 200 for already revoked or unknown tokens")
        void logout_returnsSuccessForAnyTokenState() {
            when(logoutUserUseCase.execute(any())).thenReturn(Result.success());

            var result = controller.logout(new RefreshTokenRequest("already-revoked-token"));

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().status()).isEqualTo("success");
            assertThat(result.getBody().data()).isNull();
        }
    }

    @Nested
    @DisplayName("negative")
    class Negative {

        @Test
        @DisplayName("detects blank refresh token as a validation error")
        void logout_detectsBlankRefreshToken() {
            var violations = validator.validate(new RefreshTokenRequest("   "));

            assertThat(violations)
                    .anyMatch(v -> v.getPropertyPath().toString().equals("refreshToken"));
        }

        @Test
        @DisplayName("detects null refresh token as a validation error")
        void logout_detectsNullRefreshToken() {
            var violations = validator.validate(new RefreshTokenRequest(null));

            assertThat(violations)
                    .anyMatch(v -> v.getPropertyPath().toString().equals("refreshToken"));
        }

        @Test
        @DisplayName("detects empty refresh token as a validation error")
        void logout_detectsEmptyRefreshToken() {
            var violations = validator.validate(new RefreshTokenRequest(""));

            assertThat(violations)
                    .anyMatch(v -> v.getPropertyPath().toString().equals("refreshToken"));
        }
    }

    @Nested
    @DisplayName("response contract")
    class ResponseContract {

        @Test
        @DisplayName("returns the expected JSend success wrapper without data")
        void logout_returnsExpectedJsendStructure() {
            when(logoutUserUseCase.execute(any())).thenReturn(Result.success());

            var result = controller.logout(new RefreshTokenRequest("refresh-token"));

            JsonNode json = objectMapper.valueToTree(result.getBody());
            assertThat(json.get("status").asText()).isEqualTo("success");
            assertThat(json.has("data")).isFalse();
            assertThat(json.has("accessToken")).isFalse();
            assertThat(json.has("refreshToken")).isFalse();
            assertThat(json.has("user")).isFalse();
        }
    }
}
