package io.github.phunguy65.ttbs.backend.train.domain.model;

import static org.assertj.core.api.Assertions.*;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.domain.errors.RouteSeatAvailabilityError;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RouteSeatAvailabilityTest {

    private static final RouteId ROUTE_ID = RouteId.of(UUID.randomUUID());
    private static final SeatId SEAT_ID = SeatId.of(UUID.randomUUID());

    // ── hold() ───────────────────────────────────────────────────────────────

    @Test
    void hold_whenAvailable_shouldTransitionToHeld() {
        RouteSeatAvailability availability = RouteSeatAvailability.create(ROUTE_ID, SEAT_ID);

        Result<Void, RouteSeatAvailabilityError> result = availability.hold();

        assertThat(result.isSuccess()).isTrue();
        assertThat(availability.getStatus()).isEqualTo(RouteSeatAvailabilityStatus.HELD);
    }

    @Test
    void hold_whenAlreadyHeld_shouldReturnFailure() {
        RouteSeatAvailability availability = RouteSeatAvailability.reconstitute(
                ROUTE_ID, SEAT_ID, RouteSeatAvailabilityStatus.HELD);

        Result<Void, RouteSeatAvailabilityError> result = availability.hold();

        assertThat(result.isFailure()).isTrue();
        assertThat(((Result.Failure<Void, RouteSeatAvailabilityError>) result).error())
                .isInstanceOf(RouteSeatAvailabilityError.SeatNotAvailable.class);
    }

    @Test
    void hold_whenBooked_shouldReturnFailure() {
        RouteSeatAvailability availability = RouteSeatAvailability.reconstitute(
                ROUTE_ID, SEAT_ID, RouteSeatAvailabilityStatus.BOOKED);

        Result<Void, RouteSeatAvailabilityError> result = availability.hold();

        assertThat(result.isFailure()).isTrue();
    }

    // ── confirmHold() ────────────────────────────────────────────────────────

    @Test
    void confirmHold_whenHeld_shouldTransitionToBooked() {
        RouteSeatAvailability availability = RouteSeatAvailability.reconstitute(
                ROUTE_ID, SEAT_ID, RouteSeatAvailabilityStatus.HELD);

        Result<Void, RouteSeatAvailabilityError> result = availability.confirmHold();

        assertThat(result.isSuccess()).isTrue();
        assertThat(availability.getStatus()).isEqualTo(RouteSeatAvailabilityStatus.BOOKED);
    }

    @Test
    void confirmHold_whenAvailable_shouldReturnFailure() {
        RouteSeatAvailability availability = RouteSeatAvailability.create(ROUTE_ID, SEAT_ID);

        Result<Void, RouteSeatAvailabilityError> result = availability.confirmHold();

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void confirmHold_whenBooked_shouldReturnFailure() {
        RouteSeatAvailability availability = RouteSeatAvailability.reconstitute(
                ROUTE_ID, SEAT_ID, RouteSeatAvailabilityStatus.BOOKED);

        Result<Void, RouteSeatAvailabilityError> result = availability.confirmHold();

        assertThat(result.isFailure()).isTrue();
    }

    // ── expire() ─────────────────────────────────────────────────────────────

    @Test
    void expire_whenHeld_shouldTransitionToAvailable() {
        RouteSeatAvailability availability = RouteSeatAvailability.reconstitute(
                ROUTE_ID, SEAT_ID, RouteSeatAvailabilityStatus.HELD);

        Result<Void, RouteSeatAvailabilityError> result = availability.expire();

        assertThat(result.isSuccess()).isTrue();
        assertThat(availability.getStatus()).isEqualTo(RouteSeatAvailabilityStatus.AVAILABLE);
    }

    @Test
    void expire_whenAvailable_shouldReturnFailure() {
        RouteSeatAvailability availability = RouteSeatAvailability.create(ROUTE_ID, SEAT_ID);

        Result<Void, RouteSeatAvailabilityError> result = availability.expire();

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void expire_whenBooked_shouldReturnFailure() {
        RouteSeatAvailability availability = RouteSeatAvailability.reconstitute(
                ROUTE_ID, SEAT_ID, RouteSeatAvailabilityStatus.BOOKED);

        Result<Void, RouteSeatAvailabilityError> result = availability.expire();

        assertThat(result.isFailure()).isTrue();
    }

    // ── hold() → confirmHold() full chain ────────────────────────────────────

    @Test
    void holdThenConfirm_fullChain_shouldEndAtBooked() {
        RouteSeatAvailability availability = RouteSeatAvailability.create(ROUTE_ID, SEAT_ID);

        availability.hold();
        Result<Void, RouteSeatAvailabilityError> result = availability.confirmHold();

        assertThat(result.isSuccess()).isTrue();
        assertThat(availability.getStatus()).isEqualTo(RouteSeatAvailabilityStatus.BOOKED);
    }

    @Test
    void holdThenExpire_fullChain_shouldEndAtAvailable() {
        RouteSeatAvailability availability = RouteSeatAvailability.create(ROUTE_ID, SEAT_ID);

        availability.hold();
        Result<Void, RouteSeatAvailabilityError> result = availability.expire();

        assertThat(result.isSuccess()).isTrue();
        assertThat(availability.getStatus()).isEqualTo(RouteSeatAvailabilityStatus.AVAILABLE);
    }

    // ── book() ───────────────────────────────────────────────────────────────

    @Test
    void book_whenAvailable_shouldTransitionToBooked() {
        RouteSeatAvailability availability = RouteSeatAvailability.create(ROUTE_ID, SEAT_ID);

        Result<Void, RouteSeatAvailabilityError> result = availability.book();

        assertThat(result.isSuccess()).isTrue();
        assertThat(availability.getStatus()).isEqualTo(RouteSeatAvailabilityStatus.BOOKED);
    }

    @Test
    void book_whenAlreadyBooked_shouldReturnFailure() {
        RouteSeatAvailability availability = RouteSeatAvailability.create(ROUTE_ID, SEAT_ID);
        availability.book();

        Result<Void, RouteSeatAvailabilityError> result = availability.book();

        assertThat(result.isFailure()).isTrue();
        assertThat(((Result.Failure<Void, RouteSeatAvailabilityError>) result).error())
                .isInstanceOf(RouteSeatAvailabilityError.SeatNotAvailable.class);
    }

    @Test
    void book_whenCancelled_shouldReturnFailure() {
        RouteSeatAvailability availability = RouteSeatAvailability.reconstitute(
                ROUTE_ID, SEAT_ID, RouteSeatAvailabilityStatus.CANCELLED);

        Result<Void, RouteSeatAvailabilityError> result = availability.book();

        assertThat(result.isFailure()).isTrue();
    }

    // ── cancel() ─────────────────────────────────────────────────────────────

    @Test
    void cancel_whenBooked_shouldTransitionToCancelled() {
        RouteSeatAvailability availability = RouteSeatAvailability.create(ROUTE_ID, SEAT_ID);
        availability.book();

        Result<Void, RouteSeatAvailabilityError> result = availability.cancel();

        assertThat(result.isSuccess()).isTrue();
        assertThat(availability.getStatus()).isEqualTo(RouteSeatAvailabilityStatus.CANCELLED);
    }

    @Test
    void cancel_whenAvailable_shouldReturnFailure() {
        RouteSeatAvailability availability = RouteSeatAvailability.create(ROUTE_ID, SEAT_ID);

        Result<Void, RouteSeatAvailabilityError> result = availability.cancel();

        assertThat(result.isFailure()).isTrue();
    }

    // ── release() ────────────────────────────────────────────────────────────

    @Test
    void release_whenCancelled_shouldTransitionToAvailable() {
        RouteSeatAvailability availability = RouteSeatAvailability.create(ROUTE_ID, SEAT_ID);
        availability.book();
        availability.cancel();

        Result<Void, RouteSeatAvailabilityError> result = availability.release();

        assertThat(result.isSuccess()).isTrue();
        assertThat(availability.getStatus()).isEqualTo(RouteSeatAvailabilityStatus.AVAILABLE);
    }

    @Test
    void release_whenBooked_shouldReturnFailure() {
        RouteSeatAvailability availability = RouteSeatAvailability.create(ROUTE_ID, SEAT_ID);
        availability.book();

        Result<Void, RouteSeatAvailabilityError> result = availability.release();

        assertThat(result.isFailure()).isTrue();
    }

    // ── create() / reconstitute() ────────────────────────────────────────────

    @Test
    void create_shouldInitializeWithAvailableStatus() {
        RouteSeatAvailability availability = RouteSeatAvailability.create(ROUTE_ID, SEAT_ID);

        assertThat(availability.getRouteId()).isEqualTo(ROUTE_ID);
        assertThat(availability.getSeatId()).isEqualTo(SEAT_ID);
        assertThat(availability.getStatus()).isEqualTo(RouteSeatAvailabilityStatus.AVAILABLE);
    }

    @Test
    void reconstitute_shouldRestoreAllFields() {
        RouteSeatAvailability availability = RouteSeatAvailability.reconstitute(
                ROUTE_ID, SEAT_ID, RouteSeatAvailabilityStatus.BOOKED);

        assertThat(availability.getStatus()).isEqualTo(RouteSeatAvailabilityStatus.BOOKED);
    }
}
