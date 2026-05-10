package io.github.phunguy65.ttbs.backend.booking.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.phunguy65.ttbs.backend.booking.domain.error.BookingError;
import io.github.phunguy65.ttbs.backend.booking.domain.event.BookingCancelled;
import io.github.phunguy65.ttbs.backend.booking.domain.event.BookingConfirmed;
import io.github.phunguy65.ttbs.backend.booking.domain.event.BookingCreated;
import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripId;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Booking")
class BookingTest {

    private static final BookingId BOOKING_ID =
            BookingId.of(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final UserId USER_ID =
            UserId.of(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    private static final ScheduledTripId TRIP_ID =
            ScheduledTripId.of(UUID.fromString("33333333-3333-3333-3333-333333333333"));
    private static final Money TOTAL_PRICE = Money.vnd(1_000_000L);
    private static final Instant PAYMENT_DEADLINE = Instant.now().plusSeconds(900);

    private static BookingUserInfo bookerInfo() {
        return BookingUserInfo.of("Nguyen Van A", "a@b.com", null, null, null, null, null);
    }

    private static List<BookingPassenger> twoPassengers() {
        return List.of(
                BookingPassenger.of(
                        SeatId.of(UUID.fromString("44444444-4444-4444-4444-000000000001")),
                        "Nguyen Van B",
                        "ID001",
                        LocalDate.of(1990, 1, 1),
                        "male"),
                BookingPassenger.of(
                        SeatId.of(UUID.fromString("44444444-4444-4444-4444-000000000002")),
                        "Nguyen Van C",
                        "ID002",
                        LocalDate.of(1992, 3, 15),
                        "female"));
    }

    private static Booking createHeldBooking() {
        return Booking.create(
                BOOKING_ID,
                USER_ID,
                TRIP_ID,
                bookerInfo(),
                twoPassengers(),
                TOTAL_PRICE,
                "idem-key-1",
                PAYMENT_DEADLINE);
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("returns HELD status and registers BookingCreated event")
        void create_returnsHeldStatusAndRegistersBookingCreatedEvent() {
            Booking booking = createHeldBooking();

            assertThat(booking.getStatus()).isEqualTo(BookingStatus.HELD);
            assertThat(booking.getDomainEvents()).hasSize(1);
            assertThat(booking.getDomainEvents().get(0)).isInstanceOf(BookingCreated.class);
        }
    }

    @Nested
    @DisplayName("confirm()")
    class Confirm {

        @Test
        @DisplayName("HELD → CONFIRMED registers BookingConfirmed event")
        void held_toConfirmed_registersBookingConfirmedEvent() {
            Booking booking = createHeldBooking();
            booking.clearDomainEvents();

            Result<Void, BookingError> result = booking.confirm();

            assertThat(result.isSuccess()).isTrue();
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
            assertThat(booking.getDomainEvents()).hasSize(1);
            assertThat(booking.getDomainEvents().get(0)).isInstanceOf(BookingConfirmed.class);
        }

        @Test
        @DisplayName("CONFIRMED → fails with InvalidStatusTransition")
        void confirmed_toConfirmed_fails() {
            Booking booking = createHeldBooking();
            booking.confirm();
            booking.clearDomainEvents();

            Result<Void, BookingError> result = booking.confirm();

            assertThat(result.isFailure()).isTrue();
            assertThat(((Result.Failure<?, BookingError>) result).error())
                    .isInstanceOf(BookingError.InvalidStatusTransition.class);
        }
    }

    @Nested
    @DisplayName("cancel()")
    class Cancel {

        @Test
        @DisplayName("HELD → CANCELLED sets requiresRefund=false")
        void held_toCancelled_requiresRefundFalse() {
            Booking booking = createHeldBooking();
            booking.clearDomainEvents();

            Result<Void, BookingError> result = booking.cancel();

            assertThat(result.isSuccess()).isTrue();
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
            assertThat(booking.getDomainEvents()).hasSize(1);
            BookingCancelled event =
                    (BookingCancelled) booking.getDomainEvents().get(0);
            assertThat(event.requiresRefund()).isFalse();
        }

        @Test
        @DisplayName("CONFIRMED → CANCELLED sets requiresRefund=true")
        void confirmed_toCancelled_requiresRefundTrue() {
            Booking booking = createHeldBooking();
            booking.confirm();
            booking.clearDomainEvents();

            Result<Void, BookingError> result = booking.cancel();

            assertThat(result.isSuccess()).isTrue();
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
            assertThat(booking.getDomainEvents()).hasSize(1);
            BookingCancelled event =
                    (BookingCancelled) booking.getDomainEvents().get(0);
            assertThat(event.requiresRefund()).isTrue();
        }

        @Test
        @DisplayName("CANCELLED → fails with InvalidStatusTransition")
        void cancelled_toCancelled_fails() {
            Booking booking = createHeldBooking();
            booking.cancel();
            booking.clearDomainEvents();

            Result<Void, BookingError> result = booking.cancel();

            assertThat(result.isFailure()).isTrue();
            assertThat(((Result.Failure<?, BookingError>) result).error())
                    .isInstanceOf(BookingError.InvalidStatusTransition.class);
        }
    }

    @Nested
    @DisplayName("constructor validation")
    class ConstructorValidation {

        @Test
        @DisplayName("rejects duplicate passenger ID documents")
        void constructor_rejectsDuplicatePassengerIdDocuments() {
            List<BookingPassenger> duplicatePassengers = List.of(
                    BookingPassenger.of(
                            SeatId.of(UUID.fromString("44444444-4444-4444-4444-000000000001")),
                            "Nguyen Van B",
                            "SAME_ID",
                            LocalDate.of(1990, 1, 1),
                            "male"),
                    BookingPassenger.of(
                            SeatId.of(UUID.fromString("44444444-4444-4444-4444-000000000002")),
                            "Nguyen Van C",
                            "SAME_ID",
                            LocalDate.of(1992, 3, 15),
                            "female"));

            assertThatThrownBy(() -> Booking.create(
                            BOOKING_ID,
                            USER_ID,
                            TRIP_ID,
                            bookerInfo(),
                            duplicatePassengers,
                            TOTAL_PRICE,
                            "idem-key-dup",
                            PAYMENT_DEADLINE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SAME_ID");
        }
    }
}
