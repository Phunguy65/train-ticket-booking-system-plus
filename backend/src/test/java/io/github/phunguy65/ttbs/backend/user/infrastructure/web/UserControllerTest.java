package io.github.phunguy65.ttbs.backend.user.infrastructure.web;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResult;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.GlobalExceptionHandler;
import io.github.phunguy65.ttbs.backend.user.application.dto.CreateUserResult;
import io.github.phunguy65.ttbs.backend.user.application.dto.UserDto;
import io.github.phunguy65.ttbs.backend.user.application.port.TokenProvider;
import io.github.phunguy65.ttbs.backend.user.application.usecase.CreateUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.GetUserByIdUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.ListUsersUseCase;
import io.github.phunguy65.ttbs.backend.user.domain.errors.UserError;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserRole;
import io.github.phunguy65.ttbs.backend.user.infrastructure.security.SecurityConfig;
import java.time.Instant;
import java.util.List;
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

@WebMvcTest(UserController.class)
@Import({UserRequestMapper.class, GlobalExceptionHandler.class, SecurityConfig.class})
@WithMockUser
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetUserByIdUseCase getUserByIdUseCase;

    @MockitoBean
    private CreateUserUseCase createUserUseCase;

    @MockitoBean
    private ListUsersUseCase listUsersUseCase;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private static final UUID USER_UUID = UUID.randomUUID();

    private UserDto sampleUserDto() {
        return new UserDto(
                USER_UUID, "alice@example.com", "Alice", "090", UserRole.CUSTOMER, Instant.now());
    }

    // ── POST /api/v1/users ────────────────────────────────────────────────────

    @Test
    void createUser_validRequest_shouldReturn201WithTemporaryPassword() throws Exception {
        CreateUserResult createResult =
                new CreateUserResult(sampleUserDto(), "abc123temporarypassword");
        when(createUserUseCase.execute(any())).thenReturn(Result.success(createResult));

        mockMvc.perform(post("/api/v1/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@example.com\",\"fullName\":\"Alice\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.email").value("alice@example.com"))
                .andExpect(jsonPath("$.data.temporaryPassword").value("abc123temporarypassword"));
    }

    @Test
    void createUser_duplicateEmail_shouldReturn409WithErrorCode() throws Exception {
        when(createUserUseCase.execute(any()))
                .thenReturn(Result.failure(new UserError.EmailAlreadyExists()));

        mockMvc.perform(post("/api/v1/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"dup@example.com\",\"fullName\":\"Dup User\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("USER_EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void createUser_blankEmail_shouldReturn400WithValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"fullName\":\"Alice\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data.errors[?(@.field == 'email')]").exists());
    }

    @Test
    void createUser_blankFullName_shouldReturn400WithValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@example.com\",\"fullName\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data.errors[?(@.field == 'fullName')]").exists());
    }

    // ── GET /api/v1/users/{id} ────────────────────────────────────────────────

    @Test
    void getById_userFound_shouldReturn200WithUserFields() throws Exception {
        when(getUserByIdUseCase.execute(any())).thenReturn(Result.success(sampleUserDto()));

        mockMvc.perform(get("/api/v1/users/{id}", USER_UUID).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(USER_UUID.toString()))
                .andExpect(jsonPath("$.data.email").value("alice@example.com"))
                .andExpect(jsonPath("$.data.fullName").value("Alice"))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.data.temporaryPassword").doesNotExist());
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

    // ── GET /api/v1/users ─────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void listUsers_admin_defaultParams_shouldReturn200WithSliceStructure() throws Exception {
        UserDto dto = sampleUserDto();
        PageResult<UserDto> pageResult = PageResult.of(List.of(dto), 0, 20, false);
        when(listUsersUseCase.execute(anyInt(), anyInt(), anyString(), any()))
                .thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/users").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].email").value("alice@example.com"))
                .andExpect(jsonPath("$.data.content[0].fullName").value("Alice"))
                .andExpect(jsonPath("$.data.content[0].role").value("CUSTOMER"))
                .andExpect(jsonPath("$.data.content[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.hasPrevious").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listUsers_admin_hasNext_shouldReturnHasNextTrue() throws Exception {
        UserDto dto = sampleUserDto();
        PageResult<UserDto> pageResult = PageResult.of(List.of(dto), 0, 5, true);
        when(listUsersUseCase.execute(anyInt(), anyInt(), anyString(), any()))
                .thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/users").param("size", "5").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.hasPrevious").value(false));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void listUsers_nonAdminUser_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/v1/users").with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    @org.springframework.security.test.context.support.WithAnonymousUser
    void listUsers_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/users")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listUsers_negativePage_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/v1/users").param("page", "-1").with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.message").value("page must be >= 0"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listUsers_sizeZero_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/v1/users").param("size", "0").with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.message").value("size must be between 1 and 100"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listUsers_sizeExceedsMax_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/v1/users").param("size", "200").with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.message").value("size must be between 1 and 100"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listUsers_invalidSortField_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/v1/users").param("sort", "passwordHash,asc").with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(
                        jsonPath("$.data.message").value("sort field not allowed: passwordHash"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listUsers_validSortField_email_shouldReturn200() throws Exception {
        PageResult<UserDto> pageResult = PageResult.of(List.of(sampleUserDto()), 0, 20, false);
        when(listUsersUseCase.execute(anyInt(), anyInt(), anyString(), any()))
                .thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/users").param("sort", "email,asc").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }
}
