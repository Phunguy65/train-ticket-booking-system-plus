package io.github.phunguy65.ttbs.backend.booking.domain.model;

import static org.assertj.core.api.Assertions.*;

import io.github.phunguy65.ttbs.backend.booking.domain.errors.BookingError;
import io.github.phunguy65.ttbs.backend.booking.domain.event.BookingCancelled;
import io.github.phunguy65.ttbs.backend.booking.domain.event.BookingConfirmed;
import io.github.phunguy65.ttbs.backend.booking.domain.event.SeatHoldCreated;
import io.github.phunguy65.ttbs.backend.booking.domain.event.SeatHoldExpired;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BookingTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ROUTE_ID = UUID.randomUUID();
    private static final UUID SEAT_ID_1 = UUID.randomUUID();
    private static final UUID SEAT_ID_2 = UUID.randomUUID();
    private static final BigDecimal PRICE = new BigDecimal("150000.00");
    private static final String CURRENCY = "VND";
    private static final String IDEMPOTENCY_KEY = "test-key-123";
    private static final String PASSENGER_NAME = "Nguyen Van A";
    private static final String PASSENGER_EMAIL = "test@example.com";
    private static final String PASSENGER_PHONE = "+84901234567";
    private static final String PAYMENT_REF = "PAY-REF-001";

    private static List<BookedSeat> twoSeats() {
        return List.of(
                BookedSeat.of(SeatId.of(SEAT_ID_1), new BigDecimal("150000.00")),
                BookedSeat.of(SeatId.of(SEAT_ID_2), new BigDecimal("225000.00")));
    }

    private static Booking createHold(Instant deadline) {
        BigDecimal total = new BigDecimal("375000.00");
        return Booking.createHold(
                USER_ID,
                ROUTE_ID,
                twoSeats(),
                total,
                CURRENCY,
                deadline,
                IDEMPOTENCY_KEY,
                PASSENGER_NAME,
                PASSENGER_EMAIL,
                PASSENGER_PHONE);
    }

    private static Booking createHold() {
        return createHold(Instant.now().plus(15, ChronoUnit.MINUTES));
    }

    // ── createHold() ─────────────────────────────────────────────────────────

    @Test
    void createHold_shouldStartInHeldStatus() {
        Booking booking = createHold();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.HELD);
    }

    @Test
    void createHold_shouldRegisterSeatHoldCreatedEvent() {
        Booking booking = createHold();

        assertThat(booking.getDomainEvents()).hasSize(1);
        assertThat(booking.getDomainEvents().getFirst()).isInstanceOf(SeatHoldCreated.class);
    }

    @Test
    void createHold_shouldSetCorrectFields() {
        Instant deadline = Instant.now().plus(15, ChronoUnit.MINUTES);
        Booking booking = createHold(deadline);

        assertThat(booking.getUserId().value()).isEqualTo(USER_ID);
        assertThat(booking.getRouteId().value()).isEqualTo(ROUTE_ID);
        assertThat(booking.getBookedSeats()).hasSize(2);
        assertThat(booking.getCurrency()).isEqualTo(CURRENCY);
        assertThat(booking.getIdempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
        assertThat(booking.getPaymentDeadline()).isEqualTo(deadline);
        assertThat(booking.getId()).isNotNull();
        assertThat(booking.getPaymentReference()).isNull();
    }

    @Test
    void createHold_seatHoldCreatedEvent_containsCorrectData() {
        Instant deadline = Instant.now().plus(15, ChronoUnit.MINUTES);
        Booking booking = createHold(deadline);

        SeatHoldCreated event = (SeatHoldCreated) booking.getDomainEvents().getFirst();
        assertThat(event.userId()).isEqualTo(USER_ID);
        assertThat(event.routeId()).isEqualTo(ROUTE_ID);
        assertThat(event.seatIds()).containsExactlyInAnyOrder(SEAT_ID_1, SEAT_ID_2);
        assertThat(event.expiresAt()).isEqualTo(deadline);
    }

    // ── confirm() ────────────────────────────────────────────────────────────

    @Test
    void confirm_fromHeld_withValidDeadline_shouldTransitionToConfirmed() {
        Booking booking = createHold();
        booking.clearDomainEvents();

        Result<Void, BookingError> result = booking.confirm(PAYMENT_REF);

        assertThat(result.isSuccess()).isTrue();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getPaymentReference()).isEqualTo(PAYMENT_REF);
        assertThat(booking.getDomainEvents()).hasSize(1);
        assertThat(booking.getDomainEvents().getFirst()).isInstanceOf(BookingConfirmed.class);
    }

    @Test
    void confirm_fromHeld_withExpiredDeadline_shouldReturnHoldExpired() {
        // Create hold with a deadline already in the past
        Booking booking = createHold(Instant.now().minus(1, ChronoUnit.SECONDS));

        Result<Void, BookingError> result = booking.confirm(PAYMENT_REF);

        assertThat(result.isFailure()).isTrue();
        assertThat(((Result.Failure<Void, BookingError>) result).error())
                .isInstanceOf(BookingError.HoldExpired.class);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.HELD);
    }

    @Test
    void confirm_fromConfirmed_shouldReturnInvalidStatusTransition() {
        Booking booking = createHold();
        booking.confirm(PAYMENT_REF);

        Result<Void, BookingError> result = booking.confirm(PAYMENT_REF);

        assertThat(result.isFailure()).isTrue();
        BookingError error = ((Result.Failure<Void, BookingError>) result).error();
        assertThat(error).isInstanceOf(BookingError.InvalidStatusTransition.class);
        assertThat(error.message()).contains("CONFIRMED");
    }

    @Test
    void confirm_fromCancelled_shouldReturnInvalidStatusTransition() {
        Booking booking = createHold();
        booking.cancel();

        Result<Void, BookingError> result = booking.confirm(PAYMENT_REF);

        assertThat(result.isFailure()).isTrue();
        BookingError error = ((Result.Failure<Void, BookingError>) result).error();
        assertThat(error).isInstanceOf(BookingError.InvalidStatusTransition.class);
        assertThat(error.message()).contains("CANCELLED");
    }

    // ── expire() ─────────────────────────────────────────────────────────────

    @Test
    void expire_fromHeld_shouldTransitionToCancelled() {
        Booking booking = createHold();
        booking.clearDomainEvents();

        Result<Void, BookingError> result = booking.expire();

        assertThat(result.isSuccess()).isTrue();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getDomainEvents()).hasSize(1);
        assertThat(booking.getDomainEvents().getFirst()).isInstanceOf(SeatHoldExpired.class);
    }

    @Test
    void expire_fromHeld_seatHoldExpiredEvent_containsCorrectData() {
        Booking booking = createHold();
        booking.clearDomainEvents();
        booking.expire();

        SeatHoldExpired event = (SeatHoldExpired) booking.getDomainEvents().getFirst();
        assertThat(event.userId()).isEqualTo(USER_ID);
        assertThat(event.routeId()).isEqualTo(ROUTE_ID);
        assertThat(event.seatIds()).containsExactlyInAnyOrder(SEAT_ID_1, SEAT_ID_2);
    }

    @Test
    void expire_fromCancelled_shouldReturnSuccessIdempotent() {
        Booking booking = createHold();
        booking.cancel();
        booking.clearDomainEvents();

        Result<Void, BookingError> result = booking.expire();

        assertThat(result.isSuccess()).isTrue();
        // No additional event should be registered (already cancelled)
        assertThat(booking.getDomainEvents()).isEmpty();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void expire_fromConfirmed_shouldReturnSuccessIdempotent() {
        Booking booking = createHold();
        booking.confirm(PAYMENT_REF);
        booking.clearDomainEvents();

        Result<Void, BookingError> result = booking.expire();

        // Idempotent — confirmed bookings are not expired
        assertThat(result.isSuccess()).isTrue();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getDomainEvents()).isEmpty();
    }

    // ── cancel() ─────────────────────────────────────────────────────────────

    @Test
    void cancel_fromHeld_shouldTransitionToCancelled() {
        Booking booking = createHold();
        booking.clearDomainEvents();

        Result<Void, BookingError> result = booking.cancel();

        assertThat(result.isSuccess()).isTrue();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getDomainEvents()).hasSize(1);
        assertThat(booking.getDomainEvents().getFirst()).isInstanceOf(BookingCancelled.class);
    }

    @Test
    void cancel_fromConfirmed_shouldTransitionToCancelled() {
        Booking booking = createHold();
        booking.confirm(PAYMENT_REF);
        booking.clearDomainEvents();

        Result<Void, BookingError> result = booking.cancel();

        assertThat(result.isSuccess()).isTrue();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void cancel_fromCancelled_shouldReturnSuccessIdempotent() {
        Booking booking = createHold();
        booking.cancel();
        booking.clearDomainEvents();

        Result<Void, BookingError> result = booking.cancel();

        assertThat(result.isSuccess()).isTrue();
        // No additional event registered
        assertThat(booking.getDomainEvents()).isEmpty();
    }

    // ── reconstitute() ───────────────────────────────────────────────────────

    @Test
    void reconstitute_shouldNotRegisterDomainEvents() {
        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.reconstitute(
                bookingId,
                USER_ID,
                ROUTE_ID,
                twoSeats(),
                PRICE,
                CURRENCY,
                IDEMPOTENCY_KEY,
                BookingStatus.CONFIRMED,
                Instant.now(),
                Instant.now().plus(15, ChronoUnit.MINUTES),
                PAYMENT_REF,
                PASSENGER_NAME,
                PASSENGER_EMAIL,
                PASSENGER_PHONE);

        assertThat(booking.getDomainEvents()).isEmpty();
    }

    @Test
    void reconstitute_shouldRestoreAllFields() {
        UUID bookingId = UUID.randomUUID();
        Instant createdAt = Instant.now();
        Instant deadline = Instant.now().plus(15, ChronoUnit.MINUTES);

        Booking booking = Booking.reconstitute(
                bookingId,
                USER_ID,
                ROUTE_ID,
                twoSeats(),
                PRICE,
                CURRENCY,
                IDEMPOTENCY_KEY,
                BookingStatus.CONFIRMED,
                createdAt,
                deadline,
                PAYMENT_REF,
                PASSENGER_NAME,
                PASSENGER_EMAIL,
                PASSENGER_PHONE);

        assertThat(booking.getId().value()).isEqualTo(bookingId);
        assertThat(booking.getUserId().value()).isEqualTo(USER_ID);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getCreatedAt()).isEqualTo(createdAt);
        assertThat(booking.getPaymentDeadline()).isEqualTo(deadline);
        assertThat(booking.getPaymentReference()).isEqualTo(PAYMENT_REF);
        assertThat(booking.getBookedSeats()).hasSize(2);
    }
}
