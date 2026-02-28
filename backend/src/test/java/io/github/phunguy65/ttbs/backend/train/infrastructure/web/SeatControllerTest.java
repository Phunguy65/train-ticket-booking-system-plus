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
import io.github.phunguy65.ttbs.backend.train.application.dto.SeatDto;
import io.github.phunguy65.ttbs.backend.train.application.usecase.CreateSeatUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetAvailableSeatsForRouteUseCase;
import io.github.phunguy65.ttbs.backend.train.application.usecase.GetSeatsByTrainUseCase;
import io.github.phunguy65.ttbs.backend.train.domain.errors.SeatError;
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

@WebMvcTest(SeatController.class)
@Import({
    SeatRequestMapper.class,
    GlobalExceptionHandler.class,
    SecurityConfig.class,
    WebConfig.class,
    JacksonConfig.class
})
@WithMockUser
class SeatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateSeatUseCase createSeatUseCase;

    @MockitoBean
    private GetSeatsByTrainUseCase getSeatsByTrainUseCase;

    @MockitoBean
    private GetAvailableSeatsForRouteUseCase getAvailableSeatsForRouteUseCase;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private static final UUID TRAIN_UUID = UUID.randomUUID();
    private static final UUID ROUTE_UUID = UUID.randomUUID();
    private static final UUID SEAT_UUID = UUID.randomUUID();

    private SeatDto sampleSeatDto() {
        return new SeatDto(SEAT_UUID, TRAIN_UUID, "1A", Instant.now());
    }

    // ── POST /api/v1.0/trains/{trainId}/seats ───────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void createSeat_validRequest_shouldReturn201() throws Exception {
        when(createSeatUseCase.execute(any())).thenReturn(Result.success(sampleSeatDto()));

        mockMvc.perform(post("/api/v1.0/trains/{trainId}/seats", TRAIN_UUID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seatNumber\":\"1A\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.seatNumber").value("1A"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createSeat_duplicateSeatNumber_shouldReturn409() throws Exception {
        when(createSeatUseCase.execute(any()))
                .thenReturn(Result.failure(new SeatError.SeatNumberAlreadyExists("1A")));

        mockMvc.perform(post("/api/v1.0/trains/{trainId}/seats", TRAIN_UUID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seatNumber\":\"1A\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("SEAT_NUMBER_ALREADY_EXISTS"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createSeat_trainNotFound_shouldReturn404() throws Exception {
        when(createSeatUseCase.execute(any()))
                .thenReturn(Result.failure(new SeatError.TrainNotFound()));

        mockMvc.perform(post("/api/v1.0/trains/{trainId}/seats", TRAIN_UUID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seatNumber\":\"1A\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("TRAIN_NOT_FOUND"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void createSeat_nonAdmin_shouldReturn403() throws Exception {
        mockMvc.perform(post("/api/v1.0/trains/{trainId}/seats", TRAIN_UUID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seatNumber\":\"1A\"}"))
                .andExpect(status().isForbidden());
    }

    // ── GET /api/v1.0/trains/{trainId}/seats ────────────────────────────────

    @Test
    void getSeatsByTrain_shouldReturn200WithList() throws Exception {
        when(getSeatsByTrainUseCase.execute(TRAIN_UUID)).thenReturn(List.of(sampleSeatDto()));

        mockMvc.perform(get("/api/v1.0/trains/{trainId}/seats", TRAIN_UUID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].seatNumber").value("1A"));
    }

    // ── GET /api/v1.0/routes/{routeId}/seats/available ─────────────────────

    @Test
    void getAvailableSeats_shouldReturn200WithList() throws Exception {
        when(getAvailableSeatsForRouteUseCase.execute(ROUTE_UUID))
                .thenReturn(List.of(sampleSeatDto()));

        mockMvc.perform(get("/api/v1.0/routes/{routeId}/seats/available", ROUTE_UUID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].seatNumber").value("1A"));
    }
}
