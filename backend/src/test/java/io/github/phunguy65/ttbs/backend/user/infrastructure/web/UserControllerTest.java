package io.github.phunguy65.ttbs.backend.user.infrastructure.web;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.GlobalExceptionHandler;
import io.github.phunguy65.ttbs.backend.user.application.dto.UserDto;
import io.github.phunguy65.ttbs.backend.user.application.port.TokenProvider;
import io.github.phunguy65.ttbs.backend.user.application.usecase.GetUserByIdUseCase;
import io.github.phunguy65.ttbs.backend.user.domain.errors.UserError;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserRole;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import({UserRequestMapper.class, GlobalExceptionHandler.class})
@WithMockUser
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetUserByIdUseCase getUserByIdUseCase;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private static final UUID USER_UUID = UUID.randomUUID();

    private UserDto sampleUserDto() {
        return new UserDto(
                USER_UUID, "alice@example.com", "Alice", "090", UserRole.CUSTOMER, Instant.now());
    }

    @Test
    void getById_userFound_shouldReturn200WithUserFields() throws Exception {
        when(getUserByIdUseCase.execute(any())).thenReturn(Result.success(sampleUserDto()));

        mockMvc.perform(get("/api/v1/users/{id}", USER_UUID).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(USER_UUID.toString()))
                .andExpect(jsonPath("$.data.email").value("alice@example.com"))
                .andExpect(jsonPath("$.data.fullName").value("Alice"))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"));
    }

    @Test
    void getById_userNotFound_shouldReturn404WithErrorCode() throws Exception {
        when(getUserByIdUseCase.execute(any()))
                .thenReturn(Result.failure(new UserError.UserNotFound()));

        mockMvc.perform(get("/api/v1/users/{id}", USER_UUID).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("USER_NOT_FOUND"));
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001")
    void getMe_authenticatedUser_shouldReturn200WithUserFields() throws Exception {
        when(getUserByIdUseCase.execute(any())).thenReturn(Result.success(sampleUserDto()));

        mockMvc.perform(get("/api/v1/users/me").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.email").value("alice@example.com"));
    }
}
