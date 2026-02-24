package io.github.phunguy65.ttbs.backend.booking.infrastructure.web;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.github.phunguy65.ttbs.backend.booking.application.dto.BookingDto;
import io.github.phunguy65.ttbs.backend.booking.application.usecase.CreateBookingUseCase;
import io.github.phunguy65.ttbs.backend.booking.application.usecase.GetBookingUseCase;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.GlobalExceptionHandler;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.WebConfig;
import io.github.phunguy65.ttbs.backend.user.application.port.TokenProvider;
import java.math.BigDecimal;
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
    private CreateBookingUseCase createBookingUseCase;

    @MockitoBean
    private GetBookingUseCase getBookingUseCase;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void postBookings_withValidRequest_shouldReturn201WithJsendSuccess() throws Exception {
        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID routeId = UUID.randomUUID();
        UUID seatId = UUID.randomUUID();
        CreateBookingHttpRequest request =
                new CreateBookingHttpRequest(userId, routeId, seatId, "idempotency-key-test");
        BookingDto dto = new BookingDto(
                bookingId,
                userId,
                routeId,
                seatId,
                "PENDING",
                BigDecimal.ZERO,
                "VND",
                "idempotency-key-test");
        when(createBookingUseCase.execute(any())).thenReturn(Result.success(dto));

        mockMvc.perform(post("/api/v1.0/bookings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(bookingId.toString()))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void getBooking_withExistingId_shouldReturn200WithJsendSuccess() throws Exception {
        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID routeId = UUID.randomUUID();
        UUID seatId = UUID.randomUUID();
        BookingDto dto = new BookingDto(
                bookingId, userId, routeId, seatId, "CONFIRMED", BigDecimal.ZERO, "VND", "key-123");
        when(getBookingUseCase.execute(bookingId)).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/api/v1.0/bookings/{id}", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(bookingId.toString()))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
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
}
