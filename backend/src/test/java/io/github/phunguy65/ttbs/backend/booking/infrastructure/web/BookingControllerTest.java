package io.github.phunguy65.ttbs.backend.booking.infrastructure.web;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.github.phunguy65.ttbs.backend.booking.application.response.BookingResponse;
import io.github.phunguy65.ttbs.backend.booking.application.usecase.CancelBookingUseCase;
import io.github.phunguy65.ttbs.backend.booking.application.usecase.CreateBookingUseCase;
import io.github.phunguy65.ttbs.backend.booking.domain.error.BookingError;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.GlobalExceptionHandler;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JacksonConfig;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.WebConfig;
import io.github.phunguy65.ttbs.backend.user.application.port.TokenProvider;
import io.github.phunguy65.ttbs.backend.user.infrastructure.security.SecurityConfig;
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

@WebMvcTest(BookingController.class)
@Import({
    BookingRequestMapper.class,
    GlobalExceptionHandler.class,
    SecurityConfig.class,
    WebConfig.class,
    JacksonConfig.class
})
class BookingControllerTest {

    private static final String USER_UUID_STR = "00000000-0000-0000-0000-000000000001";
    private static final UUID BOOKING_UUID = UUID.randomUUID();
    private static final UUID ROUTE_UUID = UUID.randomUUID();
    private static final UUID SEAT_UUID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateBookingUseCase createBookingUseCase;

    @MockitoBean
    private CancelBookingUseCase cancelBookingUseCase;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private BookingResponse sampleDto() {
        return new BookingResponse(
                BOOKING_UUID,
                UUID.fromString(USER_UUID_STR),
                ROUTE_UUID,
                "John Doe",
                "john@example.com",
                null,
                100_000L,
                "VND",
                BookingStatus.HELD,
                Instant.now().plusSeconds(900),
                Instant.now());
    }

    private String createRequestBody() {
        return "{\"routeId\":\"" + ROUTE_UUID + "\","
                + "\"seatIds\":[\"" + SEAT_UUID + "\"],"
                + "\"passengerName\":\"John Doe\","
                + "\"passengerEmail\":\"john@example.com\","
                + "\"idempotencyKey\":\"test-key-1\"}";
    }

    // ── POST /api/v1.0/bookings ───────────────────────────────────────────────

    @Test
    @WithMockUser(username = USER_UUID_STR)
    void create_validRequest_shouldReturn201() throws Exception {
        when(createBookingUseCase.execute(any())).thenReturn(Result.success(sampleDto()));

        mockMvc.perform(post("/api/v1.0/bookings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.status").value("HELD"));
    }

    @Test
    @WithMockUser(username = USER_UUID_STR)
    void create_seatNotAvailable_shouldReturn409() throws Exception {
        when(createBookingUseCase.execute(any()))
                .thenReturn(Result.failure(new BookingError.SeatNotAvailable()));

        mockMvc.perform(post("/api/v1.0/bookings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("SEAT_NOT_AVAILABLE"));
    }

    @Test
    @WithMockUser(username = USER_UUID_STR)
    void create_activeHoldExists_shouldReturn409() throws Exception {
        when(createBookingUseCase.execute(any()))
                .thenReturn(Result.failure(new BookingError.ActiveHoldExists()));

        mockMvc.perform(post("/api/v1.0/bookings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("fail"));
    }

    @Test
    @WithMockUser(username = USER_UUID_STR)
    void create_invalidBody_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1.0/bookings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"routeId\":null,\"seatIds\":[],\"passengerName\":\"\","
                                + "\"passengerEmail\":\"not-an-email\",\"idempotencyKey\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("VALIDATION_ERROR"));
    }

    // ── POST /api/v1.0/bookings/{id}/cancel ──────────────────────────────────

    @Test
    @WithMockUser(username = USER_UUID_STR)
    void cancel_success_shouldReturn200() throws Exception {
        when(cancelBookingUseCase.execute(any())).thenReturn(Result.success());

        mockMvc.perform(post("/api/v1.0/bookings/" + BOOKING_UUID + "/cancel").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    @WithMockUser(username = USER_UUID_STR)
    void cancel_forbidden_shouldReturn403() throws Exception {
        when(cancelBookingUseCase.execute(any()))
                .thenReturn(Result.failure(new BookingError.Forbidden()));

        mockMvc.perform(post("/api/v1.0/bookings/" + BOOKING_UUID + "/cancel").with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("fail"));
    }

    @Test
    @WithMockUser(username = USER_UUID_STR)
    void cancel_notFound_shouldReturn404() throws Exception {
        when(cancelBookingUseCase.execute(any()))
                .thenReturn(Result.failure(new BookingError.BookingNotFound()));

        mockMvc.perform(post("/api/v1.0/bookings/" + BOOKING_UUID + "/cancel").with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("BOOKING_NOT_FOUND"));
    }
}
