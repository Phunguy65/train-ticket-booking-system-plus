package io.github.phunguy65.ttbs.backend.user.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import io.github.phunguy65.ttbs.backend.user.infrastructure.web.request.UpdateAuthenticatedUserRequest;
import jakarta.validation.Valid;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@DisplayName("AuthController me security")
class AuthControllerMeSecurityTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

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
    @DisplayName("pen-test")
    class PenTest {

        @Test
        @DisplayName("handles malformed UUID in authentication principal gracefully")
        void me_handlesMalformedUuidInAuthenticationPrincipalGracefully() {
            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn("'; DROP TABLE users; --");

            assertThatThrownBy(() -> controller.me(auth))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("does not leak sensitive data in update response")
        void updateMe_doesNotLeakSensitiveDataInResponse() {
            when(updateAuthenticatedUserUseCase.execute(any()))
                    .thenReturn(Result.success(userResponse()));

            var result = controller.updateMe(authentication(), validRequest());

            JsonNode json = objectMapper.valueToTree(result.getBody());
            assertThat(json.get("data").has("passwordHash")).isFalse();
            assertThat(json.get("data").has("password")).isFalse();
        }

        @Test
        @DisplayName("XSS payload in fullName is stored as-is without execution context")
        void updateMe_acceptsXssPayloadAsOpaqueInput() {
            String xssPayload = "<script>alert('xss')</script>";
            UserResponse response = new UserResponse(
                    USER_ID,
                    "customer@example.com",
                    xssPayload,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "CUSTOMER",
                    Instant.parse("2026-05-15T10:15:30Z"));
            when(updateAuthenticatedUserUseCase.execute(any()))
                    .thenReturn(Result.success(response));

            var result = controller.updateMe(
                    authentication(),
                    new UpdateAuthenticatedUserRequest(
                            xssPayload, "customer@example.com", null, null, null, null, null));

            JsonNode json = objectMapper.valueToTree(result.getBody());
            assertThat(json.get("data").get("fullName").asText()).isEqualTo(xssPayload);
        }
    }

    @Nested
    @DisplayName("annotation check")
    class AnnotationCheck {

        @Test
        @DisplayName("declares @PreAuthorize(\"isAuthenticated()\") on me endpoint")
        void me_declaresPreAuthorizeIsAuthenticated() throws Exception {
            Method method = AuthController.class.getDeclaredMethod("me", Authentication.class);

            PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
            assertThat(annotation).isNotNull();
            assertThat(annotation.value()).isEqualTo("isAuthenticated()");
        }

        @Test
        @DisplayName("declares @PreAuthorize(\"isAuthenticated()\") on updateMe endpoint")
        void updateMe_declaresPreAuthorizeIsAuthenticated() throws Exception {
            Method method = AuthController.class.getDeclaredMethod(
                    "updateMe", Authentication.class, UpdateAuthenticatedUserRequest.class);

            PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
            assertThat(annotation).isNotNull();
            assertThat(annotation.value()).isEqualTo("isAuthenticated()");
        }

        @Test
        @DisplayName("declares @Valid on updateMe request body")
        void updateMe_declaresValidOnRequestBody() throws Exception {
            Method method = AuthController.class.getDeclaredMethod(
                    "updateMe", Authentication.class, UpdateAuthenticatedUserRequest.class);

            assertThat(Arrays.stream(method.getParameterAnnotations()[1])
                            .map(annotation -> annotation.annotationType().getName())
                            .toList())
                    .contains(Valid.class.getName());
        }
    }

    private Authentication authentication() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(USER_ID.toString());
        return auth;
    }

    private UpdateAuthenticatedUserRequest validRequest() {
        return new UpdateAuthenticatedUserRequest(
                "Safe User",
                "customer@example.com",
                "+84901234567",
                LocalDate.of(1995, 5, 15),
                "female",
                "012345678901",
                "123 Test Street");
    }

    private UserResponse userResponse() {
        return new UserResponse(
                USER_ID,
                "customer@example.com",
                "Safe User",
                "+84901234567",
                LocalDate.of(1995, 5, 15),
                "female",
                "012345678901",
                "123 Test Street",
                "CUSTOMER",
                Instant.parse("2026-05-15T10:15:30Z"));
    }

    private static final UUID USER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
}
