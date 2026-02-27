package io.github.phunguy65.ttbs.backend.user.infrastructure.web;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResult;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.GlobalExceptionHandler;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JacksonConfig;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.WebConfig;
import io.github.phunguy65.ttbs.backend.user.application.dto.CreateUserResult;
import io.github.phunguy65.ttbs.backend.user.application.dto.UserDto;
import io.github.phunguy65.ttbs.backend.user.application.port.TokenProvider;
import io.github.phunguy65.ttbs.backend.user.application.usecase.BulkSoftDeleteUsersUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.CreateUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.GetUserByIdUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.ListUsersUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.SoftDeleteUserUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.UpdateUserUseCase;
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
@Import({
    UserRequestMapper.class,
    GlobalExceptionHandler.class,
    SecurityConfig.class,
    WebConfig.class,
    JacksonConfig.class
})
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
    private UpdateUserUseCase updateUserUseCase;

    @MockitoBean
    private SoftDeleteUserUseCase softDeleteUserUseCase;

    @MockitoBean
    private BulkSoftDeleteUsersUseCase bulkSoftDeleteUsersUseCase;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private static final UUID USER_UUID = UUID.randomUUID();

    private UserDto sampleUserDto() {
        return new UserDto(
                USER_UUID, "alice@example.com", "Alice", "090", UserRole.CUSTOMER, Instant.now());
    }

    // ── POST /api/v1.0/users ─────────────────────────────────────────────────────

    @Test
    void createUser_validRequest_shouldReturn201WithTemporaryPassword() throws Exception {
        CreateUserResult createResult =
                new CreateUserResult(sampleUserDto(), "abc123temporarypassword");
        when(createUserUseCase.execute(any())).thenReturn(Result.success(createResult));

        mockMvc.perform(post("/api/v1.0/users")
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

        mockMvc.perform(post("/api/v1.0/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"dup@example.com\",\"fullName\":\"Dup User\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("USER_EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void createUser_blankEmail_shouldReturn400WithValidationError() throws Exception {
        mockMvc.perform(post("/api/v1.0/users")
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
        mockMvc.perform(post("/api/v1.0/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@example.com\",\"fullName\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data.errors[?(@.field == 'fullName')]").exists());
    }

    // ── GET /api/v1.0/users/{id} ─────────────────────────────────────────────────

    @Test
    void getById_userFound_shouldReturn200WithUserFields() throws Exception {
        when(getUserByIdUseCase.execute(any())).thenReturn(Result.success(sampleUserDto()));

        mockMvc.perform(get("/api/v1.0/users/{id}", USER_UUID).with(csrf()))
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

        mockMvc.perform(get("/api/v1.0/users/{id}", USER_UUID).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("USER_NOT_FOUND"));
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001")
    void getMe_authenticatedUser_shouldReturn200WithUserFields() throws Exception {
        when(getUserByIdUseCase.execute(any())).thenReturn(Result.success(sampleUserDto()));

        mockMvc.perform(get("/api/v1.0/users/me").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.email").value("alice@example.com"));
    }

    // ── GET /api/v1.0/users ──────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void listUsers_admin_defaultParams_shouldReturn200WithSliceStructure() throws Exception {
        UserDto dto = sampleUserDto();
        PageResult<UserDto> pageResult = PageResult.of(List.of(dto), 0, 20, false);
        when(listUsersUseCase.execute(anyInt(), anyInt(), anyString(), any()))
                .thenReturn(pageResult);

        mockMvc.perform(get("/api/v1.0/users").with(csrf()))
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

        mockMvc.perform(get("/api/v1.0/users").param("size", "5").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.hasPrevious").value(false));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void listUsers_nonAdminUser_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/v1.0/users").with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    @org.springframework.security.test.context.support.WithAnonymousUser
    void listUsers_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1.0/users")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listUsers_negativePage_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/v1.0/users").param("page", "-1").with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.message").value("page must be >= 0"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listUsers_sizeZero_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/v1.0/users").param("size", "0").with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.message").value("size must be between 1 and 100"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listUsers_sizeExceedsMax_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/v1.0/users").param("size", "200").with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.message").value("size must be between 1 and 100"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listUsers_invalidSortField_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/v1.0/users").param("sort", "passwordHash,asc").with(csrf()))
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

        mockMvc.perform(get("/api/v1.0/users").param("sort", "email,asc").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    // ── PATCH /api/v1.0/users/me ─────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001")
    void patchMe_validPartialBody_shouldReturn200WithUpdatedUser() throws Exception {
        UserDto updated = new UserDto(
                USER_UUID,
                "alice@example.com",
                "New Name",
                "090",
                UserRole.CUSTOMER,
                Instant.now());
        when(updateUserUseCase.execute(any())).thenReturn(Result.success(updated));

        mockMvc.perform(patch("/api/v1.0/users/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"New Name\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.fullName").value("New Name"));
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001")
    void patchMe_blankFullName_shouldReturn400WithRequiredViolation() throws Exception {
        mockMvc.perform(patch("/api/v1.0/users/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data.errors[?(@.field == 'fullName')]").exists());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001")
    void patchMe_invalidEmailFormat_shouldReturn400WithInvalidFormatViolation() throws Exception {
        mockMvc.perform(patch("/api/v1.0/users/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data.errors[?(@.field == 'email')]").exists());
    }

    @Test
    @org.springframework.security.test.context.support.WithAnonymousUser
    void patchMe_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(patch("/api/v1.0/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Name\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ── PATCH /api/v1.0/users/{id} ───────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void patchById_asAdmin_shouldReturn200() throws Exception {
        when(updateUserUseCase.execute(any())).thenReturn(Result.success(sampleUserDto()));

        mockMvc.perform(patch("/api/v1.0/users/{id}", USER_UUID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void patchById_asCustomer_shouldReturn403() throws Exception {
        mockMvc.perform(patch("/api/v1.0/users/{id}", USER_UUID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Updated\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void patchById_userNotFound_shouldReturn404() throws Exception {
        when(updateUserUseCase.execute(any()))
                .thenReturn(Result.failure(new UserError.UserNotFound()));

        mockMvc.perform(patch("/api/v1.0/users/{id}", USER_UUID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Updated\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("USER_NOT_FOUND"));
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001")
    void patchMe_duplicateEmail_shouldReturn409() throws Exception {
        when(updateUserUseCase.execute(any()))
                .thenReturn(Result.failure(new UserError.EmailAlreadyExists()));

        mockMvc.perform(patch("/api/v1.0/users/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"taken@example.com\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("USER_EMAIL_ALREADY_EXISTS"));
    }

    // ── DELETE /api/v1.0/users/me ────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001")
    void deleteMe_authenticated_shouldReturn200() throws Exception {
        when(softDeleteUserUseCase.execute(any())).thenReturn(Result.success());

        mockMvc.perform(delete("/api/v1.0/users/me").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    @org.springframework.security.test.context.support.WithAnonymousUser
    void deleteMe_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(delete("/api/v1.0/users/me")).andExpect(status().isUnauthorized());
    }

    // ── DELETE /api/v1.0/users/{id} ──────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteById_asAdmin_shouldReturn200() throws Exception {
        when(softDeleteUserUseCase.execute(any())).thenReturn(Result.success());

        mockMvc.perform(delete("/api/v1.0/users/{id}", USER_UUID).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void deleteById_asCustomer_shouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/v1.0/users/{id}", USER_UUID).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteById_userNotFound_shouldReturn404() throws Exception {
        when(softDeleteUserUseCase.execute(any()))
                .thenReturn(Result.failure(new UserError.UserNotFound()));

        mockMvc.perform(delete("/api/v1.0/users/{id}", USER_UUID).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.data.code").value("USER_NOT_FOUND"));
    }

    // ── DELETE /api/v1.0/users (bulk) ────────────────────────────────────────────

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "ADMIN")
    void bulkDelete_asAdmin_shouldReturn200WithDeletedCount() throws Exception {
        UUID otherId = UUID.randomUUID();
        when(bulkSoftDeleteUsersUseCase.execute(any())).thenReturn(1);

        mockMvc.perform(delete("/api/v1.0/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userIds\":[\"" + otherId + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.deletedCount").value(1));
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "ADMIN")
    void bulkDelete_selfInList_shouldReturn400() throws Exception {
        mockMvc.perform(delete("/api/v1.0/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userIds\":[\"00000000-0000-0000-0000-000000000001\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.code").value("USER_CANNOT_BULK_DELETE_SELF"));
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "ADMIN")
    void bulkDelete_moreThan100Ids_shouldReturn400() throws Exception {
        StringBuilder sb = new StringBuilder("{\"userIds\":[");
        for (int i = 0; i < 101; i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(UUID.randomUUID()).append("\"");
        }
        sb.append("]}");

        mockMvc.perform(delete("/api/v1.0/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sb.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.code").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void bulkDelete_asCustomer_shouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/v1.0/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userIds\":[\"" + UUID.randomUUID() + "\"]}"))
                .andExpect(status().isForbidden());
    }
}
