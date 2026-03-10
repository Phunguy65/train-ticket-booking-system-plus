package io.github.phunguy65.ttbs.backend.train.infrastructure.web;

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
import io.github.phunguy65.ttbs.backend.train.application.dto.TrainDto;
import io.github.phunguy65.ttbs.backend.train.application.usecase.BulkSoftDeleteTrainsUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.CreateTrainUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetTrainByIdUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetTrainsUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.SoftDeleteTrainUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.UpdateTrainUseCase;
import io.github.phunguy65.ttbs.backend.train.domain.error.TrainError;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TrainController.class)
@Import({
    TrainRequestMapper.class,
    GlobalExceptionHandler.class,
    SecurityConfig.class,
    WebConfig.class,
    JacksonConfig.class
})
@WithMockUser
class TrainControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateTrainUseCase createTrainUseCase;

    @MockitoBean
    private GetTrainByIdUseCase getTrainByIdUseCase;

    @MockitoBean
    private GetTrainsUseCase getTrainsUseCase;

    @MockitoBean
    private UpdateTrainUseCase updateTrainUseCase;

    @MockitoBean
    private SoftDeleteTrainUseCase softDeleteTrainUseCase;

    @MockitoBean
    private BulkSoftDeleteTrainsUseCase bulkSoftDeleteTrainsUseCase;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private static final UUID TRAIN_UUID = UUID.randomUUID();

    private TrainDto sampleTrainDto() {
        return new TrainDto(TRAIN_UUID, "SE001", "Reunification Express", 250, Instant.now());
    }

    // ── POST /api/v1.0/trains ────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void createTrain_validRequest_shouldReturn201() throws Exception {
        when(createTrainUseCase.execute(any())).thenReturn(Result.success(sampleTrainDto()));

        mockMvc.perform(
                        post("/api/v1.0/trains")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"trainNumber\":\"SE001\",\"name\":\"Reunification Express\",\"totalSeats\":250}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.trainNumber").value("SE001"))
                .andExpect(jsonPath("$.data.name").value("Reunification Express"))
                .andExpect(jsonPath("$.data.totalSeats").value(250));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createTrain_duplicateTrainNumber_shouldReturn409() throws Exception {
        when(createTrainUseCase.execute(any()))
                .thenReturn(Result.failure(new TrainError.TrainNumberAlreadyExists("SE001")));

        mockMvc.perform(
                        post("/api/v1.0/trains")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"trainNumber\":\"SE001\",\"name\":\"Duplicate\",\"totalSeats\":100}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("TRAIN_NUMBER_ALREADY_EXISTS"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createTrain_invalidRequest_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1.0/trains")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trainNumber\":\"\",\"name\":\"Test\",\"totalSeats\":100}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void createTrain_nonAdmin_shouldReturn403() throws Exception {
        mockMvc.perform(post("/api/v1.0/trains")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"trainNumber\":\"SE001\",\"name\":\"Test\",\"totalSeats\":100}"))
                .andExpect(status().isForbidden());
    }

    // ── GET /api/v1.0/trains ─────────────────────────────────────────────────

    @Test
    void listTrains_defaultParams_shouldReturn200WithSliceStructure() throws Exception {
        TrainDto dto = sampleTrainDto();
        PageResult<TrainDto> pageResult = PageResult.of(List.of(dto), 0, 20, false);
        when(getTrainsUseCase.execute(anyInt(), anyInt(), anyString(), any()))
                .thenReturn(pageResult);

        mockMvc.perform(get("/api/v1.0/trains").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].trainNumber").value("SE001"))
                .andExpect(jsonPath("$.data.content[0].name").value("Reunification Express"))
                .andExpect(jsonPath("$.data.content[0].totalSeats").value(250))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.hasPrevious").value(false));
    }

    // ── GET /api/v1.0/trains/{id} ────────────────────────────────────────────

    @Test
    void getById_found_shouldReturn200() throws Exception {
        when(getTrainByIdUseCase.execute(any())).thenReturn(Result.success(sampleTrainDto()));

        mockMvc.perform(get("/api/v1.0/trains/{id}", TRAIN_UUID).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(TRAIN_UUID.toString()))
                .andExpect(jsonPath("$.data.trainNumber").value("SE001"))
                .andExpect(jsonPath("$.data.name").value("Reunification Express"));
    }

    @Test
    void getById_notFound_shouldReturn404() throws Exception {
        when(getTrainByIdUseCase.execute(any()))
                .thenReturn(Result.failure(new TrainError.TrainNotFound()));

        mockMvc.perform(get("/api/v1.0/trains/{id}", TRAIN_UUID).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("TRAIN_NOT_FOUND"));
    }

    // ── PATCH /api/v1.0/trains/{id} ──────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void patchTrain_validRequest_shouldReturn200() throws Exception {
        when(updateTrainUseCase.execute(any())).thenReturn(Result.success(sampleTrainDto()));

        mockMvc.perform(patch("/api/v1.0/trains/{id}", TRAIN_UUID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated Express\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.trainNumber").value("SE001"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void patchTrain_trainNotFound_shouldReturn404() throws Exception {
        when(updateTrainUseCase.execute(any()))
                .thenReturn(Result.failure(new TrainError.TrainNotFound()));

        mockMvc.perform(patch("/api/v1.0/trains/{id}", TRAIN_UUID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("TRAIN_NOT_FOUND"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void patchTrain_duplicateTrainNumber_shouldReturn409() throws Exception {
        when(updateTrainUseCase.execute(any()))
                .thenReturn(Result.failure(new TrainError.TrainNumberAlreadyExists("SE002")));

        mockMvc.perform(patch("/api/v1.0/trains/{id}", TRAIN_UUID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trainNumber\":\"SE002\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("TRAIN_NUMBER_ALREADY_EXISTS"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void patchTrain_nonAdmin_shouldReturn403() throws Exception {
        mockMvc.perform(patch("/api/v1.0/trains/{id}", TRAIN_UUID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void patchTrain_blankTrainNumber_shouldReturn400() throws Exception {
        mockMvc.perform(patch("/api/v1.0/trains/{id}", TRAIN_UUID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trainNumber\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("VALIDATION_ERROR"));
    }
}
