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
import io.github.phunguy65.ttbs.backend.train.application.dto.RouteDto;
import io.github.phunguy65.ttbs.backend.train.application.usecase.BulkSoftDeleteRoutesUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.CreateRouteUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetRouteByIdUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetRoutesUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.SoftDeleteRouteUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.UpdateRouteUseCase;
import io.github.phunguy65.ttbs.backend.train.domain.errors.RouteError;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteStatus;
import io.github.phunguy65.ttbs.backend.user.application.port.TokenProvider;
import io.github.phunguy65.ttbs.backend.user.infrastructure.security.SecurityConfig;
import java.math.BigDecimal;
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

@WebMvcTest(RouteController.class)
@Import({
    RouteRequestMapper.class,
    GlobalExceptionHandler.class,
    SecurityConfig.class,
    WebConfig.class,
    JacksonConfig.class
})
@WithMockUser
class RouteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateRouteUseCase createRouteUseCase;

    @MockitoBean
    private GetRouteByIdUseCase getRouteByIdUseCase;

    @MockitoBean
    private GetRoutesUseCase getRoutesUseCase;

    @MockitoBean
    private UpdateRouteUseCase updateRouteUseCase;

    @MockitoBean
    private SoftDeleteRouteUseCase softDeleteRouteUseCase;

    @MockitoBean
    private BulkSoftDeleteRoutesUseCase bulkSoftDeleteRoutesUseCase;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private static final UUID ROUTE_UUID = UUID.randomUUID();
    private static final UUID TRAIN_UUID = UUID.randomUUID();
    private static final UUID ORIGIN_UUID = UUID.randomUUID();
    private static final UUID DEST_UUID = UUID.randomUUID();

    private RouteDto sampleRouteDto() {
        return new RouteDto(
                ROUTE_UUID,
                TRAIN_UUID,
                ORIGIN_UUID,
                DEST_UUID,
                Instant.parse("2025-06-01T08:00:00Z"),
                Instant.parse("2025-06-01T12:00:00Z"),
                new BigDecimal("150.00"),
                RouteStatus.SCHEDULED,
                Instant.now());
    }

    private String createRouteJson() {
        return String.format(
                "{\"trainId\":\"%s\",\"originStationId\":\"%s\",\"destinationStationId\":\"%s\","
                        + "\"departureTime\":\"2025-06-01T08:00:00Z\",\"arrivalTime\":\"2025-06-01T12:00:00Z\","
                        + "\"basePrice\":150.00}",
                TRAIN_UUID, ORIGIN_UUID, DEST_UUID);
    }

    // ── POST /api/v1.0/routes ────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void createRoute_validRequest_shouldReturn201() throws Exception {
        when(createRouteUseCase.execute(any())).thenReturn(Result.success(sampleRouteDto()));

        mockMvc.perform(post("/api/v1.0/routes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRouteJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(ROUTE_UUID.toString()))
                .andExpect(jsonPath("$.data.trainId").value(TRAIN_UUID.toString()))
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createRoute_missingField_shouldReturn400() throws Exception {
        // Missing arrivalTime
        String badJson = String.format(
                "{\"trainId\":\"%s\",\"originStationId\":\"%s\",\"destinationStationId\":\"%s\","
                        + "\"departureTime\":\"2025-06-01T08:00:00Z\",\"basePrice\":150.00}",
                TRAIN_UUID, ORIGIN_UUID, DEST_UUID);

        mockMvc.perform(post("/api/v1.0/routes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void createRoute_nonAdmin_shouldReturn403() throws Exception {
        mockMvc.perform(post("/api/v1.0/routes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRouteJson()))
                .andExpect(status().isForbidden());
    }

    // ── GET /api/v1.0/routes/{id} ────────────────────────────────────────────

    @Test
    void getById_found_shouldReturn200() throws Exception {
        when(getRouteByIdUseCase.execute(any())).thenReturn(Result.success(sampleRouteDto()));

        mockMvc.perform(get("/api/v1.0/routes/{id}", ROUTE_UUID).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(ROUTE_UUID.toString()))
                .andExpect(jsonPath("$.data.trainId").value(TRAIN_UUID.toString()))
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"));
    }

    @Test
    void getById_notFound_shouldReturn404() throws Exception {
        when(getRouteByIdUseCase.execute(any()))
                .thenReturn(Result.failure(new RouteError.RouteNotFound()));

        mockMvc.perform(get("/api/v1.0/routes/{id}", ROUTE_UUID).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("ROUTE_NOT_FOUND"));
    }

    // ── GET /api/v1.0/routes ─────────────────────────────────────────────────

    @Test
    void listRoutes_defaultParams_shouldReturn200WithSliceStructure() throws Exception {
        RouteDto dto = sampleRouteDto();
        PageResult<RouteDto> pageResult = PageResult.of(List.of(dto), 0, 20, false);
        when(getRoutesUseCase.execute(anyInt(), anyInt(), anyString(), any(), any()))
                .thenReturn(pageResult);

        mockMvc.perform(get("/api/v1.0/routes").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].id").value(ROUTE_UUID.toString()))
                .andExpect(jsonPath("$.data.content[0].status").value("SCHEDULED"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.hasPrevious").value(false));
    }

    @Test
    void listRoutes_invalidPage_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/v1.0/routes").param("page", "-1").with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("VALIDATION_ERROR"));
    }

    @Test
    void listRoutes_invalidSortField_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/v1.0/routes")
                        .param("sort", "invalidField,desc")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("VALIDATION_ERROR"));
    }

    @Test
    void listRoutes_invalidSize_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/v1.0/routes").param("size", "0").with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("VALIDATION_ERROR"));
    }

    // ── PATCH /api/v1.0/routes/{id} ──────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void patchRoute_validRequest_shouldReturn200() throws Exception {
        when(updateRouteUseCase.execute(any())).thenReturn(Result.success(sampleRouteDto()));

        mockMvc.perform(patch("/api/v1.0/routes/{id}", ROUTE_UUID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"basePrice\":200.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(ROUTE_UUID.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void patchRoute_routeNotFound_shouldReturn404() throws Exception {
        when(updateRouteUseCase.execute(any()))
                .thenReturn(Result.failure(new RouteError.RouteNotFound()));

        mockMvc.perform(patch("/api/v1.0/routes/{id}", ROUTE_UUID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"basePrice\":200.00}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("ROUTE_NOT_FOUND"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void patchRoute_nonAdmin_shouldReturn403() throws Exception {
        mockMvc.perform(patch("/api/v1.0/routes/{id}", ROUTE_UUID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"basePrice\":200.00}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void patchRoute_negativePrice_shouldReturn400() throws Exception {
        mockMvc.perform(patch("/api/v1.0/routes/{id}", ROUTE_UUID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"basePrice\":-10.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("VALIDATION_ERROR"));
    }

    // ── DELETE /api/v1.0/routes/{id} ─────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteById_found_shouldReturn200() throws Exception {
        when(softDeleteRouteUseCase.execute(any())).thenReturn(Result.success());

        mockMvc.perform(delete("/api/v1.0/routes/{id}", ROUTE_UUID).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteById_notFound_shouldReturn404() throws Exception {
        when(softDeleteRouteUseCase.execute(any()))
                .thenReturn(Result.failure(new RouteError.RouteNotFound()));

        mockMvc.perform(delete("/api/v1.0/routes/{id}", ROUTE_UUID).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("ROUTE_NOT_FOUND"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void deleteById_nonAdmin_shouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/v1.0/routes/{id}", ROUTE_UUID).with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ── POST /api/v1.0/routes:bulkDelete ─────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void bulkDelete_validRequest_shouldReturn200() throws Exception {
        when(bulkSoftDeleteRoutesUseCase.execute(any())).thenReturn(Result.success(2));

        mockMvc.perform(post("/api/v1.0/routes:bulkDelete")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"routeIds\":[\"" + UUID.randomUUID() + "\",\""
                                + UUID.randomUUID() + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.deletedCount").value(2));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void bulkDelete_emptyArray_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1.0/routes:bulkDelete")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"routeIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void bulkDelete_invalidIds_shouldReturn422() throws Exception {
        when(bulkSoftDeleteRoutesUseCase.execute(any()))
                .thenReturn(Result.failure(new RouteError.RoutesNotFound(List.of(ROUTE_UUID))));

        mockMvc.perform(post("/api/v1.0/routes:bulkDelete")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"routeIds\":[\"" + ROUTE_UUID + "\"]}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("ROUTES_NOT_FOUND"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void bulkDelete_nonAdmin_shouldReturn403() throws Exception {
        mockMvc.perform(post("/api/v1.0/routes:bulkDelete")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"routeIds\":[\"" + UUID.randomUUID() + "\"]}"))
                .andExpect(status().isForbidden());
    }
}
