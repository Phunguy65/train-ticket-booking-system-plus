package io.github.phunguy65.ttbs.backend.user.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.user.application.usecase.DeleteAuthenticatedUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.GetAuthenticatedUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.LoginUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.LogoutUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.RefreshTokenUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.RegisterUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.UpdateAuthenticatedUserUseCase;
import java.lang.reflect.Method;
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

@DisplayName("AuthController deleteMe security")
class AuthControllerDeleteMeSecurityTest {

    private static final UUID USER_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(
                mock(RegisterUserUseCase.class),
                mock(LoginUserUseCase.class),
                mock(RefreshTokenUseCase.class),
                mock(LogoutUserUseCase.class),
                mock(GetAuthenticatedUserUseCase.class),
                mock(UpdateAuthenticatedUserUseCase.class),
                mock(DeleteAuthenticatedUserUseCase.class));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                new MockHttpServletRequest("DELETE", "/api/v1.0/auth/me")));
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
        void deleteMe_handlesMalformedUuidInAuthenticationPrincipalGracefully() {
            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn("'; DROP TABLE users; --");

            assertThatThrownBy(() -> controller.deleteMe(auth))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("handles null authentication name gracefully")
        void deleteMe_handlesNullAuthenticationNameGracefully() {
            Authentication auth = mock(Authentication.class);
            when(auth.getName()).thenReturn(null);

            assertThatThrownBy(() -> controller.deleteMe(auth))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("annotation check")
    class AnnotationCheck {

        @Test
        @DisplayName("declares @PreAuthorize(\"isAuthenticated()\") on deleteMe endpoint")
        void deleteMe_declaresPreAuthorizeIsAuthenticated() throws Exception {
            Method method =
                    AuthController.class.getDeclaredMethod("deleteMe", Authentication.class);

            PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
            assertThat(annotation).isNotNull();
            assertThat(annotation.value()).isEqualTo("isAuthenticated()");
        }

        @Test
        @DisplayName("deleteMe does not accept request body")
        void deleteMe_doesNotAcceptRequestBody() throws Exception {
            Method method =
                    AuthController.class.getDeclaredMethod("deleteMe", Authentication.class);

            assertThat(method.getParameterCount()).isEqualTo(1);
            assertThat(method.getParameterTypes()).containsExactly(Authentication.class);
        }
    }
}
