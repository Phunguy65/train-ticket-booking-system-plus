package io.github.phunguy65.ttbs.backend.station.infrastructure.web;

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
import io.github.phunguy65.ttbs.backend.station.application.dto.StationDto;
import io.github.phunguy65.ttbs.backend.station.application.usecase.BulkSoftDeleteStationsUseCase;
import io.github.phunguy65.ttbs.backend.station.application.usecase.CreateStationUseCase;
import io.github.phunguy65.ttbs.backend.station.application.usecase.GetStationByIdUseCase;
import io.github.phunguy65.ttbs.backend.station.application.usecase.GetStationsUseCase;
import io.github.phunguy65.ttbs.backend.station.application.usecase.SoftDeleteStationUseCase;
import io.github.phunguy65.ttbs.backend.station.application.usecase.UpdateStationUseCase;
import io.github.phunguy65.ttbs.backend.station.domain.error.StationError;
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

@WebMvcTest(StationController.class)
@Import({
    StationRequestMapper.class,
    GlobalExceptionHandler.class,
    SecurityConfig.class,
    WebConfig.class,
    JacksonConfig.class
})
@WithMockUser
class StationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateStationUseCase createStationUseCase;

    @MockitoBean
    private GetStationByIdUseCase getStationByIdUseCase;

    @MockitoBean
    private GetStationsUseCase getStationsUseCase;

    @MockitoBean
    private UpdateStationUseCase updateStationUseCase;

    @MockitoBean
    private SoftDeleteStationUseCase softDeleteStationUseCase;

    @MockitoBean
    private BulkSoftDeleteStationsUseCase bulkSoftDeleteStationsUseCase;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private static final UUID STATION_UUID = UUID.randomUUID();

    private StationDto sampleStationDto() {
        return new StationDto(STATION_UUID, "HN", "Hanoi Station", "Hanoi", Instant.now());
    }

    // ── POST /api/v1.0/stations ──────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void createStation_validRequest_shouldReturn201() throws Exception {
        when(createStationUseCase.execute(any())).thenReturn(Result.success(sampleStationDto()));

        mockMvc.perform(post("/api/v1.0/stations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"HN\",\"name\":\"Hanoi Station\",\"city\":\"Hanoi\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.code").value("HN"))
                .andExpect(jsonPath("$.data.name").value("Hanoi Station"))
                .andExpect(jsonPath("$.data.city").value("Hanoi"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createStation_duplicateCode_shouldReturn409() throws Exception {
        when(createStationUseCase.execute(any()))
                .thenReturn(Result.failure(new StationError.StationCodeAlreadyExists("HN")));

        mockMvc.perform(post("/api/v1.0/stations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"HN\",\"name\":\"Duplicate\",\"city\":\"Hanoi\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("STATION_CODE_ALREADY_EXISTS"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createStation_invalidRequest_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1.0/stations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"\",\"name\":\"Test\",\"city\":\"Test\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void createStation_nonAdmin_shouldReturn403() throws Exception {
        mockMvc.perform(post("/api/v1.0/stations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"HN\",\"name\":\"Test\",\"city\":\"Test\"}"))
                .andExpect(status().isForbidden());
    }

    // ── GET /api/v1.0/stations ───────────────────────────────────────────────

    @Test
    void listStations_defaultParams_shouldReturn200WithSliceStructure() throws Exception {
        StationDto dto = sampleStationDto();
        PageResult<StationDto> pageResult = PageResult.of(List.of(dto), 0, 20, false);
        when(getStationsUseCase.execute(anyInt(), anyInt(), anyString(), any()))
                .thenReturn(pageResult);

        mockMvc.perform(get("/api/v1.0/stations").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].code").value("HN"))
                .andExpect(jsonPath("$.data.content[0].name").value("Hanoi Station"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.hasPrevious").value(false));
    }

    @Test
    void listStations_invalidPageParam_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/v1.0/stations").param("page", "-1").with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("VALIDATION_ERROR"));
    }

    @Test
    void listStations_invalidSortField_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/v1.0/stations").param("sort", "unknown,asc").with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("VALIDATION_ERROR"));
    }

    // ── GET /api/v1.0/stations/{id} ──────────────────────────────────────────

    @Test
    void getById_found_shouldReturn200() throws Exception {
        when(getStationByIdUseCase.execute(any())).thenReturn(Result.success(sampleStationDto()));

        mockMvc.perform(get("/api/v1.0/stations/{id}", STATION_UUID).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(STATION_UUID.toString()))
                .andExpect(jsonPath("$.data.code").value("HN"))
                .andExpect(jsonPath("$.data.name").value("Hanoi Station"));
    }

    @Test
    void getById_notFound_shouldReturn404() throws Exception {
        when(getStationByIdUseCase.execute(any()))
                .thenReturn(Result.failure(new StationError.StationNotFound()));

        mockMvc.perform(get("/api/v1.0/stations/{id}", STATION_UUID).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("STATION_NOT_FOUND"));
    }

    // ── PATCH /api/v1.0/stations/{id} ────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void patchStation_validRequest_shouldReturn200() throws Exception {
        when(updateStationUseCase.execute(any())).thenReturn(Result.success(sampleStationDto()));

        mockMvc.perform(patch("/api/v1.0/stations/{id}", STATION_UUID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hanoi Central\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.code").value("HN"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void patchStation_stationNotFound_shouldReturn404() throws Exception {
        when(updateStationUseCase.execute(any()))
                .thenReturn(Result.failure(new StationError.StationNotFound()));

        mockMvc.perform(patch("/api/v1.0/stations/{id}", STATION_UUID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("STATION_NOT_FOUND"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void patchStation_duplicateCode_shouldReturn409() throws Exception {
        when(updateStationUseCase.execute(any()))
                .thenReturn(Result.failure(new StationError.StationCodeAlreadyExists("SGN")));

        mockMvc.perform(patch("/api/v1.0/stations/{id}", STATION_UUID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"SGN\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("STATION_CODE_ALREADY_EXISTS"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void patchStation_nonAdmin_shouldReturn403() throws Exception {
        mockMvc.perform(patch("/api/v1.0/stations/{id}", STATION_UUID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void patchStation_blankCode_shouldReturn400() throws Exception {
        mockMvc.perform(patch("/api/v1.0/stations/{id}", STATION_UUID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("VALIDATION_ERROR"));
    }
}
