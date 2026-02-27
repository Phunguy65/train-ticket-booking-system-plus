package io.github.phunguy65.ttbs.backend.booking.infrastructure.web;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.github.phunguy65.ttbs.backend.booking.application.dto.HoldDto;
import io.github.phunguy65.ttbs.backend.booking.application.usecase.CancelBookingUseCase;
import io.github.phunguy65.ttbs.backend.booking.application.usecase.ConfirmSeatHoldUseCase;
import io.github.phunguy65.ttbs.backend.booking.application.usecase.CreateSeatHoldUseCase;
import io.github.phunguy65.ttbs.backend.booking.application.usecase.GetBookingUseCase;
import io.github.phunguy65.ttbs.backend.booking.domain.errors.BookingError;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.GlobalExceptionHandler;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.WebConfig;
import io.github.phunguy65.ttbs.backend.user.application.port.TokenProvider;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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

@WebMvcTest(BookingController.class)
@Import({BookingRequestMapper.class, GlobalExceptionHandler.class, WebConfig.class})
@WithMockUser
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreateSeatHoldUseCase createSeatHoldUseCase;

    @MockitoBean
    private ConfirmSeatHoldUseCase confirmSeatHoldUseCase;

    @MockitoBean
    private CancelBookingUseCase cancelBookingUseCase;

    @MockitoBean
    private GetBookingUseCase getBookingUseCase;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private UserDetailsService userDetailsService;

    // ── Helpers ──────────────────────────────────────────────────────────────

    private HoldDto buildHoldDto(UUID bookingId, UUID routeId, String status) {
        List<HoldDto.BookedSeatDto> seats =
                List.of(new HoldDto.BookedSeatDto(UUID.randomUUID(), BigDecimal.valueOf(100_000)));
        return new HoldDto(
                bookingId,
                status,
                routeId,
                seats,
                BigDecimal.valueOf(100_000),
                "VND",
                status.equals("HELD") ? Instant.now().plusSeconds(900) : null);
    }

    // ── POST /hold ────────────────────────────────────────────────────────────

    @Test
    void holdSeats_withValidRequest_shouldReturn201WithJsendSuccess() throws Exception {
        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID routeId = UUID.randomUUID();
        UUID seatId = UUID.randomUUID();

        CreateSeatHoldHttpRequest request = new CreateSeatHoldHttpRequest(
                userId,
                routeId,
                List.of(seatId),
                "idem-key-1",
                "Nguyen Van A",
                "a@example.com",
                null);

        HoldDto dto = buildHoldDto(bookingId, routeId, "HELD");
        when(createSeatHoldUseCase.execute(any())).thenReturn(Result.success(dto));

        mockMvc.perform(post("/api/v1.0/bookings/hold")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(bookingId.toString()))
                .andExpect(jsonPath("$.data.status").value("HELD"))
                .andExpect(jsonPath("$.data.seats[0].unitPrice").value(100000));
    }

    @Test
    void holdSeats_whenActiveHoldExists_shouldReturn409Conflict() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID routeId = UUID.randomUUID();

        CreateSeatHoldHttpRequest request = new CreateSeatHoldHttpRequest(
                userId,
                routeId,
                List.of(UUID.randomUUID()),
                "idem-key-2",
                "Nguyen Van B",
                "b@example.com",
                null);

        when(createSeatHoldUseCase.execute(any()))
                .thenReturn(Result.failure(new BookingError.ActiveHoldExists()));

        mockMvc.perform(post("/api/v1.0/bookings/hold")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("SEAT_NOT_AVAILABLE"));
    }

    @Test
    void holdSeats_whenSeatsLocked_shouldReturn409Conflict() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID routeId = UUID.randomUUID();

        CreateSeatHoldHttpRequest request = new CreateSeatHoldHttpRequest(
                userId,
                routeId,
                List.of(UUID.randomUUID()),
                "idem-key-3",
                "Nguyen Van C",
                "c@example.com",
                null);

        when(createSeatHoldUseCase.execute(any()))
                .thenReturn(Result.failure(new BookingError.SeatsLocked()));

        mockMvc.perform(post("/api/v1.0/bookings/hold")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("SEAT_NOT_AVAILABLE"));
    }

    // ── POST /{id}/confirm ────────────────────────────────────────────────────

    @Test
    void confirmHold_withValidRequest_shouldReturn200WithJsendSuccess() throws Exception {
        UUID bookingId = UUID.randomUUID();
        UUID routeId = UUID.randomUUID();

        ConfirmSeatHoldHttpRequest request = new ConfirmSeatHoldHttpRequest("PAY-REF-123");
        HoldDto dto = buildHoldDto(bookingId, routeId, "CONFIRMED");

        when(confirmSeatHoldUseCase.execute(any())).thenReturn(Result.success(dto));

        mockMvc.perform(post("/api/v1.0/bookings/{id}/confirm", bookingId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test
    void confirmHold_whenHoldExpired_shouldReturn409Conflict() throws Exception {
        UUID bookingId = UUID.randomUUID();

        ConfirmSeatHoldHttpRequest request = new ConfirmSeatHoldHttpRequest("PAY-REF-456");
        when(confirmSeatHoldUseCase.execute(any()))
                .thenReturn(Result.failure(new BookingError.HoldExpired()));

        mockMvc.perform(post("/api/v1.0/bookings/{id}/confirm", bookingId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("BOOKING_CANNOT_CONFIRM"));
    }

    @Test
    void confirmHold_whenBookingNotFound_shouldReturn404() throws Exception {
        UUID bookingId = UUID.randomUUID();

        ConfirmSeatHoldHttpRequest request = new ConfirmSeatHoldHttpRequest("PAY-REF-789");
        when(confirmSeatHoldUseCase.execute(any()))
                .thenReturn(Result.failure(new BookingError.InvalidStatusTransition(null)));

        mockMvc.perform(post("/api/v1.0/bookings/{id}/confirm", bookingId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("BOOKING_NOT_FOUND"));
    }

    // ── DELETE /{id} ──────────────────────────────────────────────────────────

    @Test
    void cancelBooking_withValidId_shouldReturn200WithJsendSuccess() throws Exception {
        UUID bookingId = UUID.randomUUID();
        UUID routeId = UUID.randomUUID();

        HoldDto dto = buildHoldDto(bookingId, routeId, "CANCELLED");
        when(cancelBookingUseCase.execute(any())).thenReturn(Result.success(dto));

        mockMvc.perform(delete("/api/v1.0/bookings/{id}", bookingId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    void cancelBooking_whenBookingNotFound_shouldReturn404() throws Exception {
        UUID bookingId = UUID.randomUUID();

        when(cancelBookingUseCase.execute(any()))
                .thenReturn(Result.failure(new BookingError.InvalidStatusTransition(null)));

        mockMvc.perform(delete("/api/v1.0/bookings/{id}", bookingId).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("BOOKING_NOT_FOUND"));
    }

    // ── GET /{id} ─────────────────────────────────────────────────────────────

    @Test
    void getBooking_withExistingId_shouldReturn200WithJsendSuccess() throws Exception {
        UUID bookingId = UUID.randomUUID();
        UUID routeId = UUID.randomUUID();

        HoldDto dto = buildHoldDto(bookingId, routeId, "CONFIRMED");
        when(getBookingUseCase.execute(bookingId)).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/api/v1.0/bookings/{id}", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(bookingId.toString()))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.seats").isArray());
    }

    @Test
    void getBooking_withUnknownId_shouldReturn404WithJsendFail() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(getBookingUseCase.execute(unknownId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1.0/bookings/{id}", unknownId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("BOOKING_NOT_FOUND"));
    }

    // ── Pessimistic lock → 409 (via GlobalExceptionHandler) ──────────────────

    @Test
    void holdSeats_whenLockTimeoutThrown_shouldReturn409Conflict() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID routeId = UUID.randomUUID();

        CreateSeatHoldHttpRequest request = new CreateSeatHoldHttpRequest(
                userId,
                routeId,
                List.of(UUID.randomUUID()),
                "idem-key-lock",
                "Nguyen Van D",
                "d@example.com",
                null);

        when(createSeatHoldUseCase.execute(any()))
                .thenThrow(new org.springframework.dao.CannotAcquireLockException("lock timeout"));

        mockMvc.perform(post("/api/v1.0/bookings/hold")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("SEAT_NOT_AVAILABLE"));
    }
}
