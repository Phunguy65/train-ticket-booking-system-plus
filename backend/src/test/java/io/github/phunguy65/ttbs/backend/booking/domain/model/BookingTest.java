package io.github.phunguy65.ttbs.backend.booking.domain.model;

import static org.assertj.core.api.Assertions.*;

import io.github.phunguy65.ttbs.backend.booking.domain.error.BookingError;
import io.github.phunguy65.ttbs.backend.booking.domain.event.BookingCancelled;
import io.github.phunguy65.ttbs.backend.booking.domain.event.BookingConfirmed;
import io.github.phunguy65.ttbs.backend.booking.domain.event.BookingCreated;
import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BookingTest {

    private static final BookingId BOOKING_ID = BookingId.of(UUID.randomUUID());
    private static final UserId USER_ID = UserId.of(UUID.randomUUID());
    private static final RouteId ROUTE_ID = RouteId.of(UUID.randomUUID());
    private static final Instant DEADLINE = Instant.now().plusSeconds(900);

    private Booking newHeldBooking() {
        return Booking.create(
                BOOKING_ID,
                USER_ID,
                ROUTE_ID,
                "John Doe",
                "john@example.com",
                null,
                Money.vnd(100_000L),
                "VND",
                "idem-key-1",
                DEADLINE);
    }

    // ── create() ─────────────────────────────────────────────────────────────

    @Test
    void create_shouldInitializeWithHeldStatus() {
        Booking booking = newHeldBooking();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.HELD);
        assertThat(booking.getBookingId()).isEqualTo(BOOKING_ID);
        assertThat(booking.getUserId()).isEqualTo(USER_ID);
        assertThat(booking.getRouteId()).isEqualTo(ROUTE_ID);
    }

    @Test
    void create_shouldRegisterBookingCreatedEvent() {
        Booking booking = newHeldBooking();

        assertThat(booking.getDomainEvents()).hasSize(1);
        assertThat(booking.getDomainEvents().getFirst()).isInstanceOf(BookingCreated.class);
    }

    // ── confirm() ────────────────────────────────────────────────────────────

    @Test
    void confirm_whenHeld_shouldTransitionToConfirmed() {
        Booking booking = newHeldBooking();
        booking.clearDomainEvents();

        Result<Void, BookingError> result = booking.confirm();

        assertThat(result.isSuccess()).isTrue();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void confirm_whenHeld_shouldRegisterBookingConfirmedEvent() {
        Booking booking = newHeldBooking();
        booking.clearDomainEvents();

        booking.confirm();

        assertThat(booking.getDomainEvents()).hasSize(1);
        assertThat(booking.getDomainEvents().getFirst()).isInstanceOf(BookingConfirmed.class);
    }

    @Test
    void confirm_whenConfirmed_shouldReturnFailure() {
        Booking booking = newHeldBooking();
        booking.confirm();
        booking.clearDomainEvents();

        Result<Void, BookingError> result = booking.confirm();

        assertThat(result.isFailure()).isTrue();
        assertThat(((Result.Failure<Void, BookingError>) result).error())
                .isInstanceOf(BookingError.InvalidStatusTransition.class);
    }

    @Test
    void confirm_whenCancelled_shouldReturnFailure() {
        Booking booking = newHeldBooking();
        booking.cancel();
        booking.clearDomainEvents();

        Result<Void, BookingError> result = booking.confirm();

        assertThat(result.isFailure()).isTrue();
    }

    // ── cancel() ─────────────────────────────────────────────────────────────

    @Test
    void cancel_whenHeld_shouldTransitionToCancelled() {
        Booking booking = newHeldBooking();
        booking.clearDomainEvents();

        Result<Void, BookingError> result = booking.cancel();

        assertThat(result.isSuccess()).isTrue();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void cancel_whenHeld_shouldRegisterEventWithRequiresRefundFalse() {
        Booking booking = newHeldBooking();
        booking.clearDomainEvents();

        booking.cancel();

        BookingCancelled event = (BookingCancelled) booking.getDomainEvents().getFirst();
        assertThat(event.requiresRefund()).isFalse();
    }

    @Test
    void cancel_whenConfirmed_shouldRegisterEventWithRequiresRefundTrue() {
        Booking booking = newHeldBooking();
        booking.confirm();
        booking.clearDomainEvents();

        booking.cancel();

        BookingCancelled event = (BookingCancelled) booking.getDomainEvents().getFirst();
        assertThat(event.requiresRefund()).isTrue();
    }

    @Test
    void cancel_whenAlreadyCancelled_shouldReturnFailure() {
        Booking booking = newHeldBooking();
        booking.cancel();
        booking.clearDomainEvents();

        Result<Void, BookingError> result = booking.cancel();

        assertThat(result.isFailure()).isTrue();
        assertThat(((Result.Failure<Void, BookingError>) result).error())
                .isInstanceOf(BookingError.InvalidStatusTransition.class);
    }

    // ── reconstitute() ───────────────────────────────────────────────────────

    @Test
    void reconstitute_shouldNotRegisterEvents() {
        Booking booking = Booking.reconstitute(
                BOOKING_ID,
                USER_ID,
                ROUTE_ID,
                "John Doe",
                "john@example.com",
                null,
                Money.vnd(100_000L),
                "VND",
                BookingStatus.HELD,
                "idem-key-1",
                DEADLINE,
                Instant.now());

        assertThat(booking.getDomainEvents()).isEmpty();
    }

    @Test
    void reconstitute_shouldRestoreAllFields() {
        Instant createdAt = Instant.now().minusSeconds(60);
        Booking booking = Booking.reconstitute(
                BOOKING_ID,
                USER_ID,
                ROUTE_ID,
                "Jane Doe",
                "jane@example.com",
                "+84901234567",
                Money.vnd(200_000L),
                "VND",
                BookingStatus.CONFIRMED,
                "idem-key-2",
                DEADLINE,
                createdAt);

        assertThat(booking.getPassengerName()).isEqualTo("Jane Doe");
        assertThat(booking.getPassengerPhone()).isEqualTo("+84901234567");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getCreatedAt()).isEqualTo(createdAt);
    }
}
