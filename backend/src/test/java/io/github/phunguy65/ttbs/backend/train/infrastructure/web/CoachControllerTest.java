package io.github.phunguy65.ttbs.backend.train.infrastructure.web;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.GlobalExceptionHandler;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JacksonConfig;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.WebConfig;
import io.github.phunguy65.ttbs.backend.train.application.response.CoachResponse;
import io.github.phunguy65.ttbs.backend.train.application.usecase.BulkCreateCoachesUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.BulkSoftDeleteCoachesUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.CreateCoachUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetCoachByIdUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetCoachesByTrainUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.SoftDeleteCoachUseCase;
import io.github.phunguy65.ttbs.backend.train.domain.error.CoachError;
import io.github.phunguy65.ttbs.backend.user.application.port.TokenProvider;
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
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CoachController.class)
@Import({
    CoachRequestMapper.class,
    GlobalExceptionHandler.class,
    SecurityConfig.class,
    WebConfig.class,
    JacksonConfig.class
})
@WithMockUser
class CoachControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateCoachUseCase createCoachUseCase;

    @MockitoBean
    private GetCoachByIdUseCase getCoachByIdUseCase;

    @MockitoBean
    private GetCoachesByTrainUseCase getCoachesByTrainUseCase;

    @MockitoBean
    private SoftDeleteCoachUseCase softDeleteCoachUseCase;

    @MockitoBean
    private BulkSoftDeleteCoachesUseCase bulkSoftDeleteCoachesUseCase;

    @MockitoBean
    private BulkCreateCoachesUseCase bulkCreateCoachesUseCase;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private static final UUID TRAIN_UUID = UUID.randomUUID();
    private static final UUID COACH_UUID = UUID.randomUUID();

    private CoachResponse sampleCoachDto() {
        return new CoachResponse(COACH_UUID, TRAIN_UUID, 1, 50, Instant.now());
    }

    // ── POST /api/v1.0/trains/{trainId}/coaches ──────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCoach_validRequest_shouldReturn201() throws Exception {
        when(createCoachUseCase.execute(any())).thenReturn(Result.success(sampleCoachDto()));

        mockMvc.perform(post("/api/v1.0/trains/{trainId}/coaches", TRAIN_UUID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carNumber\":1,\"totalSeats\":50}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.carNumber").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCoach_carNumberZero_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1.0/trains/{trainId}/coaches", TRAIN_UUID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carNumber\":0,\"totalSeats\":50}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void createCoach_nonAdmin_shouldReturn403() throws Exception {
        mockMvc.perform(post("/api/v1.0/trains/{trainId}/coaches", TRAIN_UUID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carNumber\":1,\"totalSeats\":50}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCoach_trainNotFound_shouldReturn404() throws Exception {
        when(createCoachUseCase.execute(any()))
                .thenReturn(Result.failure(new CoachError.TrainNotFound()));

        mockMvc.perform(post("/api/v1.0/trains/{trainId}/coaches", TRAIN_UUID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carNumber\":1,\"totalSeats\":50}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("COACH_TRAIN_NOT_FOUND"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCoach_duplicateCarNumber_shouldReturn409() throws Exception {
        when(createCoachUseCase.execute(any()))
                .thenReturn(Result.failure(new CoachError.CarNumberAlreadyExists(1)));

        mockMvc.perform(post("/api/v1.0/trains/{trainId}/coaches", TRAIN_UUID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carNumber\":1,\"totalSeats\":50}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("COACH_CAR_NUMBER_ALREADY_EXISTS"));
    }

    // ── GET /api/v1.0/trains/{trainId}/coaches ───────────────────────────────

    @Test
    void getCoachesByTrain_shouldReturn200WithList() throws Exception {
        when(getCoachesByTrainUseCase.execute(any())).thenReturn(List.of(sampleCoachDto()));

        mockMvc.perform(get("/api/v1.0/trains/{trainId}/coaches", TRAIN_UUID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].carNumber").value(1));
    }

    // ── GET /api/v1.0/trains/{trainId}/coaches/{id} ──────────────────────────

    @Test
    void getCoachById_found_shouldReturn200() throws Exception {
        when(getCoachByIdUseCase.execute(any(), any()))
                .thenReturn(Result.success(sampleCoachDto()));

        mockMvc.perform(get("/api/v1.0/trains/{trainId}/coaches/{id}", TRAIN_UUID, COACH_UUID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.carNumber").value(1));
    }

    @Test
    void getCoachById_notFound_shouldReturn404() throws Exception {
        when(getCoachByIdUseCase.execute(any(), any()))
                .thenReturn(Result.failure(new CoachError.CoachNotFound()));

        mockMvc.perform(get("/api/v1.0/trains/{trainId}/coaches/{id}", TRAIN_UUID, COACH_UUID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("COACH_NOT_FOUND"));
    }

    // ── DELETE /api/v1.0/trains/{trainId}/coaches/{id} ───────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteById_success_shouldReturn200() throws Exception {
        when(softDeleteCoachUseCase.execute(any())).thenReturn(Result.success());

        mockMvc.perform(delete("/api/v1.0/trains/{trainId}/coaches/{id}", TRAIN_UUID, COACH_UUID)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteById_alreadyDeleted_shouldReturn200() throws Exception {
        // Idempotent: already-deleted coach returns success
        when(softDeleteCoachUseCase.execute(any())).thenReturn(Result.success());

        mockMvc.perform(delete("/api/v1.0/trains/{trainId}/coaches/{id}", TRAIN_UUID, COACH_UUID)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteById_notFound_shouldReturn404() throws Exception {
        when(softDeleteCoachUseCase.execute(any()))
                .thenReturn(Result.failure(new CoachError.CoachNotFound()));

        mockMvc.perform(delete("/api/v1.0/trains/{trainId}/coaches/{id}", TRAIN_UUID, COACH_UUID)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("COACH_NOT_FOUND"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteById_coachInUse_shouldReturn422WithConflictingIds() throws Exception {
        when(softDeleteCoachUseCase.execute(any()))
                .thenReturn(Result.failure(new CoachError.CoachInUse(List.of(COACH_UUID))));

        mockMvc.perform(delete("/api/v1.0/trains/{trainId}/coaches/{id}", TRAIN_UUID, COACH_UUID)
                        .with(csrf()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("COACH_IN_USE"))
                .andExpect(jsonPath("$.data.conflictingIds[0]").value(COACH_UUID.toString()));
    }

    @Test
    @WithAnonymousUser
    void deleteById_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(delete("/api/v1.0/trains/{trainId}/coaches/{id}", TRAIN_UUID, COACH_UUID)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void deleteById_nonAdmin_shouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/v1.0/trains/{trainId}/coaches/{id}", TRAIN_UUID, COACH_UUID)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ── POST /api/v1.0/coaches:bulkDelete ────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void bulkDelete_success_shouldReturn200WithDeletedCount() throws Exception {
        when(bulkSoftDeleteCoachesUseCase.execute(any())).thenReturn(Result.success(2));

        mockMvc.perform(post("/api/v1.0/coaches:bulkDelete")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"coachIds\":[\"" + COACH_UUID + "\",\"" + UUID.randomUUID()
                                + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.deletedCount").value(2));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void bulkDelete_coachInUse_shouldReturn422WithConflictingIds() throws Exception {
        UUID conflictingId = UUID.randomUUID();
        when(bulkSoftDeleteCoachesUseCase.execute(any()))
                .thenReturn(Result.failure(new CoachError.CoachInUse(List.of(conflictingId))));

        mockMvc.perform(post("/api/v1.0/coaches:bulkDelete")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"coachIds\":[\"" + COACH_UUID + "\",\"" + conflictingId
                                + "\"]}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("COACH_IN_USE"))
                .andExpect(jsonPath("$.data.conflictingIds[0]").value(conflictingId.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void bulkDelete_missingIds_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1.0/coaches:bulkDelete")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"coachIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void bulkDelete_exceedsMaxBatchSize_shouldReturn400() throws Exception {
        StringBuilder sb = new StringBuilder("{\"coachIds\":[");
        for (int i = 0; i < 101; i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(UUID.randomUUID()).append("\"");
        }
        sb.append("]}");
        mockMvc.perform(post("/api/v1.0/coaches:bulkDelete")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sb.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("VALIDATION_ERROR"));
    }

    @Test
    @WithAnonymousUser
    void bulkDelete_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/v1.0/coaches:bulkDelete")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"coachIds\":[\"" + COACH_UUID + "\"]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void bulkDelete_nonAdmin_shouldReturn403() throws Exception {
        mockMvc.perform(post("/api/v1.0/coaches:bulkDelete")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"coachIds\":[\"" + COACH_UUID + "\"]}"))
                .andExpect(status().isForbidden());
    }
}
