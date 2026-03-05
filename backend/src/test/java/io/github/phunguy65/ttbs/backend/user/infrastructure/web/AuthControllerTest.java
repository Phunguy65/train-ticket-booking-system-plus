package io.github.phunguy65.ttbs.backend.user.infrastructure.web;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.GlobalExceptionHandler;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.WebConfig;
import io.github.phunguy65.ttbs.backend.user.application.dto.LoginResultDto;
import io.github.phunguy65.ttbs.backend.user.application.dto.UserDto;
import io.github.phunguy65.ttbs.backend.user.application.port.TokenProvider;
import io.github.phunguy65.ttbs.backend.user.application.usecase.LoginUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.LogoutUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.RefreshTokenUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.RegisterUserUseCase;
import io.github.phunguy65.ttbs.backend.user.domain.error.UserError;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserRole;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(AuthController.class)
@Import({AuthRequestMapper.class, GlobalExceptionHandler.class, WebConfig.class})
@WithMockUser
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RegisterUserUseCase registerUserUseCase;

    @MockitoBean
    private LoginUserUseCase loginUserUseCase;

    @MockitoBean
    private RefreshTokenUseCase refreshTokenUseCase;

    @MockitoBean
    private LogoutUserUseCase logoutUserUseCase;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private UserDto sampleUserDto() {
        return new UserDto(
                UUID.randomUUID(),
                "test@example.com",
                "Test User",
                "090",
                UserRole.CUSTOMER,
                Instant.now());
    }

    @Test
    void register_withValidRequest_shouldReturn201() throws Exception {
        when(registerUserUseCase.execute(any())).thenReturn(Result.success(sampleUserDto()));

        mockMvc.perform(
                        post("/api/v1.0/auth/register")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"test@example.com\",\"password\":\"password123\",\"fullName\":\"Test User\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"));
    }

    @Test
    void register_duplicateEmail_shouldReturn409() throws Exception {
        when(registerUserUseCase.execute(any()))
                .thenReturn(Result.failure(new UserError.EmailAlreadyExists()));

        mockMvc.perform(
                        post("/api/v1.0/auth/register")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"dup@example.com\",\"password\":\"password123\",\"fullName\":\"Dup User\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("fail"));
    }

    @Test
    void login_withValidCredentials_shouldReturn200WithTokens() throws Exception {
        LoginResultDto loginResult =
                new LoginResultDto("access-token", "refresh-token", sampleUserDto());
        when(loginUserUseCase.execute(any())).thenReturn(Result.success(loginResult));

        mockMvc.perform(post("/api/v1.0/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"));
    }

    @Test
    void login_withBadCredentials_shouldReturn401() throws Exception {
        when(loginUserUseCase.execute(any()))
                .thenReturn(Result.failure(new UserError.InvalidCredentials()));

        mockMvc.perform(post("/api/v1.0/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("fail"));
    }

    @Test
    void register_withMissingEmail_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1.0/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"password123\",\"fullName\":\"Test User\"}"))
                .andExpect(status().isBadRequest());
    }
}
