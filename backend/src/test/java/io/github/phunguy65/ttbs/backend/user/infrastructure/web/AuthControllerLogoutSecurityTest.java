package io.github.phunguy65.ttbs.backend.user.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.user.application.usecase.DeleteAuthenticatedUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.GetAuthenticatedUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.LoginUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.LogoutUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.RefreshTokenUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.RegisterUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.UpdateAuthenticatedUserUseCase;
import io.github.phunguy65.ttbs.backend.user.infrastructure.web.request.RefreshTokenRequest;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@DisplayName("AuthController logout security")
class AuthControllerLogoutSecurityTest {

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
    @DisplayName("input handling")
    class InputHandling {

        @Test
        @DisplayName("accepts SQL injection-like refresh token as inert input")
        void logout_acceptsSqlInjectionLikeRefreshToken() {
            String token = "'; DROP TABLE refresh_tokens; --";
            when(logoutUserUseCase.execute(any())).thenReturn(Result.success());

            assertThat(validator.validate(new RefreshTokenRequest(token))).isEmpty();
            assertThatCode(() -> controller.logout(new RefreshTokenRequest(token)))
                    .doesNotThrowAnyException();
            assertThat(controller.logout(new RefreshTokenRequest(token)).getStatusCode())
                    .isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("accepts oversized refresh token without a server error")
        void logout_acceptsOversizedRefreshToken() {
            String token = "a".repeat(10000);
            when(logoutUserUseCase.execute(any())).thenReturn(Result.success());

            assertThat(validator.validate(new RefreshTokenRequest(token))).isEmpty();
            assertThatCode(() -> controller.logout(new RefreshTokenRequest(token)))
                    .doesNotThrowAnyException();
            assertThat(controller.logout(new RefreshTokenRequest(token)).getStatusCode())
                    .isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("accepts XSS-like refresh token as inert input")
        void logout_acceptsXssLikeRefreshToken() {
            String token = "<script>alert('xss')</script>";
            when(logoutUserUseCase.execute(any())).thenReturn(Result.success());

            assertThat(validator.validate(new RefreshTokenRequest(token))).isEmpty();
            assertThatCode(() -> controller.logout(new RefreshTokenRequest(token)))
                    .doesNotThrowAnyException();
            assertThat(controller.logout(new RefreshTokenRequest(token)).getStatusCode())
                    .isEqualTo(HttpStatus.OK);
        }
    }

    @Nested
    @DisplayName("response hardening")
    class ResponseHardening {

        @Test
        @DisplayName("does not echo the original refresh token in the success response")
        void logout_doesNotEchoRefreshToken() {
            String token = "sensitive-refresh-token";
            when(logoutUserUseCase.execute(any())).thenReturn(Result.success());

            var result = controller.logout(new RefreshTokenRequest(token));

            JsonNode json = objectMapper.valueToTree(result.getBody());
            assertThat(json.toString()).doesNotContain(token);
        }

        @Test
        @DisplayName("returns null data without session details")
        void logout_returnsNullDataWithoutInformationLeakage() {
            when(logoutUserUseCase.execute(any())).thenReturn(Result.success());

            var result = controller.logout(new RefreshTokenRequest("refresh-token"));

            JsonNode json = objectMapper.valueToTree(result.getBody());
            assertThat(json.has("data")).isFalse();
            assertThat(json.has("accessToken")).isFalse();
            assertThat(json.has("refreshToken")).isFalse();
            assertThat(json.has("user")).isFalse();
        }
    }

    @Nested
    @DisplayName("annotation check")
    class AnnotationCheck {

        @Test
        @DisplayName("declares @Valid on the logout controller request body")
        void logout_declaresValidOnRequestBody() throws Exception {
            Method logoutMethod =
                    AuthController.class.getDeclaredMethod("logout", RefreshTokenRequest.class);

            assertThat(Arrays.stream(logoutMethod.getParameterAnnotations()[0])
                            .map(annotation -> annotation.annotationType().getName())
                            .toList())
                    .contains(Valid.class.getName());
        }
    }
}
