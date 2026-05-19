package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.phunguy65.ttbs.backend.TestContainerConfiguration;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainerConfiguration.class)
@Transactional
class BookingDetailRepositoryQueriesTest {

    private static final Instant CREATED_AT = Instant.parse("2026-04-01T09:00:00Z");
    private static final UUID USER_ID = UUID.fromString("21111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_USER_ID =
            UUID.fromString("21111111-1111-1111-1111-111111111112");
    private static final UUID ORIGIN_ID = UUID.fromString("21111111-1111-1111-1111-111111111113");
    private static final UUID DESTINATION_ID =
            UUID.fromString("21111111-1111-1111-1111-111111111114");

    private static final UUID ROUTE_ID = UUID.fromString("21111111-1111-1111-1111-111111111115");
    private static final UUID TRAIN_ID = UUID.fromString("21111111-1111-1111-1111-111111111116");
    private static final UUID TRIP_ID = UUID.fromString("21111111-1111-1111-1111-111111111117");
    private static final UUID DELETED_TRIP_ID =
            UUID.fromString("21111111-1111-1111-1111-111111111118");
    private static final UUID BOOKING_ID = UUID.fromString("21111111-1111-1111-1111-111111111119");
    private static final UUID EXPIRED_BOOKING_ID =
            UUID.fromString("21111111-1111-1111-1111-111111111120");
    private static final UUID COACH_1_ID = UUID.fromString("21111111-1111-1111-1111-111111111121");
    private static final UUID COACH_2_ID = UUID.fromString("21111111-1111-1111-1111-111111111122");
    private static final UUID DELETED_COACH_ID =
            UUID.fromString("21111111-1111-1111-1111-111111111123");
    private static final UUID SEAT_A2_ID = UUID.fromString("21111111-1111-1111-1111-111111111124");
    private static final UUID SEAT_A1_ID = UUID.fromString("21111111-1111-1111-1111-111111111125");
    private static final UUID SEAT_B1_ID = UUID.fromString("21111111-1111-1111-1111-111111111126");
    private static final UUID DELETED_SEAT_ID =
            UUID.fromString("21111111-1111-1111-1111-111111111127");
    private static final UUID HELD_SEAT_ID =
            UUID.fromString("21111111-1111-1111-1111-111111111128");
    private static final UUID EXPIRED_HELD_SEAT_ID =
            UUID.fromString("21111111-1111-1111-1111-111111111129");
    private static final UUID AVAILABLE_SEAT_ID =
            UUID.fromString("21111111-1111-1111-1111-111111111130");
    private static final UUID DELETED_COACH_SEAT_ID =
            UUID.fromString("21111111-1111-1111-1111-111111111131");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RouteSeatAvailabilityJpaRepository routeSeatAvailabilityJpaRepository;

    @Autowired
    private ScheduledTripJpaRepository scheduledTripJpaRepository;

    @Test
    void findBookedSeatSummariesByBookingIdReturnsSortedActiveSeatsOnly() {
        insertGraph();

        var seats =
                routeSeatAvailabilityJpaRepository.findBookedSeatSummariesByBookingId(BOOKING_ID);

        assertThat(seats)
                .extracting(view -> view.getCoachNumber() + ":" + view.getSeatNumber())
                .containsExactly("1:A1", "1:A2", "2:B1", "2:B2");
        assertThat(seats)
                .extracting(BookedSeatSummaryView::getSeatId)
                .doesNotContain(DELETED_SEAT_ID, DELETED_COACH_SEAT_ID);
        assertThat(seats)
                .extracting(BookedSeatSummaryView::getPriceAtBooking)
                .containsOnly(225_000L);
    }

    @Test
    void findEnrichedByIdIncludingDeletedIncludesDeletedTripAndCalculatesAvailability() {
        insertGraph();

        var activeTrip = scheduledTripJpaRepository.findEnrichedByIdIncludingDeleted(TRIP_ID);
        var deletedTrip =
                scheduledTripJpaRepository.findEnrichedByIdIncludingDeleted(DELETED_TRIP_ID);
        var hiddenFromActiveQuery = scheduledTripJpaRepository.findEnrichedById(DELETED_TRIP_ID);

        assertThat(activeTrip).isPresent();
        assertThat(activeTrip.orElseThrow().getDurationMinutes()).isEqualTo(240);
        assertThat(activeTrip.orElseThrow().getAvailableSeatCount()).isEqualTo(2);
        assertThat(activeTrip.orElseThrow().getTrainNumber()).isEqualTo("TSE1");
        assertThat(activeTrip.orElseThrow().getOriginStationCode()).isEqualTo("TSGN");
        assertThat(deletedTrip).isPresent();
        assertThat(hiddenFromActiveQuery).isEmpty();
    }

    private void insertGraph() {
        insertUser();
        insertStations();
        insertTrain();
        insertRouteTemplate();
        insertScheduledTrips();
        insertCoaches();
        insertSeats();
        insertBookings();
        insertSeatAvailability();
    }

    private void insertUser() {
        jdbcTemplate.update(
                """
                INSERT INTO users (id, email, password_hash, full_name, role, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                USER_ID,
                "booking-detail@example.com",
                "test-password-hash",
                "Test User",
                "CUSTOMER",
                Timestamp.from(CREATED_AT),
                Timestamp.from(CREATED_AT));
        jdbcTemplate.update(
                """
                INSERT INTO users (id, email, password_hash, full_name, role, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                OTHER_USER_ID,
                "booking-detail-2@example.com",
                "test-password-hash",
                "Other Test User",
                "CUSTOMER",
                Timestamp.from(CREATED_AT),
                Timestamp.from(CREATED_AT));
    }

    private void insertStations() {
        insertStation(ORIGIN_ID, "TSGN", "Sai Gon", "Ho Chi Minh");
        insertStation(DESTINATION_ID, "TDAD", "Da Nang", "Da Nang");
    }

    private void insertStation(UUID id, String code, String name, String city) {
        jdbcTemplate.update(
                "INSERT INTO stations (id, code, name, city, created_at) VALUES (?, ?, ?, ?, ?)",
                id,
                code,
                name,
                city,
                Timestamp.from(CREATED_AT));
    }

    private void insertTrain() {
        jdbcTemplate.update(
                "INSERT INTO trains (id, train_number, name, total_seats, created_at) VALUES (?, ?, ?, ?, ?)",
                TRAIN_ID,
                "TSE1",
                "North-South Express",
                200,
                Timestamp.from(CREATED_AT));
    }

    private void insertRouteTemplate() {
        jdbcTemplate.update(
                """
                INSERT INTO route_templates (id, origin_station_id, destination_station_id, base_price, created_at)
                VALUES (?, ?, ?, ?, ?)
                """, ROUTE_ID, ORIGIN_ID, DESTINATION_ID, 450_000, Timestamp.from(CREATED_AT));
    }

    private void insertScheduledTrips() {
        jdbcTemplate.update(
                """
                INSERT INTO scheduled_trips (id, route_template_id, train_id, departure_time, arrival_time, status, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                TRIP_ID,
                ROUTE_ID,
                TRAIN_ID,
                Timestamp.from(Instant.parse("2026-05-01T08:00:00Z")),
                Timestamp.from(Instant.parse("2026-05-01T12:00:00Z")),
                "SCHEDULED",
                Timestamp.from(CREATED_AT));
        jdbcTemplate.update(
                """
                INSERT INTO scheduled_trips (id, route_template_id, train_id, departure_time, arrival_time, status, created_at, deleted_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                DELETED_TRIP_ID,
                ROUTE_ID,
                TRAIN_ID,
                Timestamp.from(Instant.parse("2026-06-01T08:00:00Z")),
                Timestamp.from(Instant.parse("2026-06-01T12:00:00Z")),
                "SCHEDULED",
                Timestamp.from(CREATED_AT),
                Timestamp.from(Instant.parse("2026-04-10T00:00:00Z")));
    }

    private void insertCoaches() {
        insertCoach(COACH_1_ID, 1, null);
        insertCoach(COACH_2_ID, 2, null);
        insertCoach(DELETED_COACH_ID, 3, Instant.parse("2026-04-10T00:00:00Z"));
    }

    private void insertCoach(UUID id, int carNumber, Instant deletedAt) {
        jdbcTemplate.update(
                "INSERT INTO coaches (id, train_id, car_number, total_seats, created_at, deleted_at) VALUES (?, ?, ?, ?, ?, ?)",
                id,
                TRAIN_ID,
                carNumber,
                50,
                Timestamp.from(CREATED_AT),
                deletedAt == null ? null : Timestamp.from(deletedAt));
    }

    private void insertSeats() {
        insertSeat(SEAT_A2_ID, COACH_1_ID, "A2", null);
        insertSeat(SEAT_A1_ID, COACH_1_ID, "A1", null);
        insertSeat(SEAT_B1_ID, COACH_2_ID, "B1", null);
        insertSeat(DELETED_SEAT_ID, COACH_1_ID, "A9", Instant.parse("2026-04-10T00:00:00Z"));
        insertSeat(HELD_SEAT_ID, COACH_2_ID, "B2", null);
        insertSeat(EXPIRED_HELD_SEAT_ID, COACH_2_ID, "B3", null);
        insertSeat(AVAILABLE_SEAT_ID, COACH_2_ID, "B4", null);
        insertSeat(DELETED_COACH_SEAT_ID, DELETED_COACH_ID, "C1", null);
    }

    private void insertSeat(UUID id, UUID coachId, String seatNumber, Instant deletedAt) {
        jdbcTemplate.update(
                "INSERT INTO seats (id, coach_id, seat_number, created_at, deleted_at) VALUES (?, ?, ?, ?, ?)",
                id,
                coachId,
                seatNumber,
                Timestamp.from(CREATED_AT),
                deletedAt == null ? null : Timestamp.from(deletedAt));
    }

    private void insertBookings() {
        insertBooking(BOOKING_ID, USER_ID, TRIP_ID, Instant.now().plusSeconds(3_600));
        insertBooking(
                EXPIRED_BOOKING_ID, OTHER_USER_ID, TRIP_ID, Instant.parse("2026-03-01T09:15:00Z"));
    }

    private void insertBooking(UUID bookingId, UUID userId, UUID tripId, Instant paymentDeadline) {
        jdbcTemplate.update(
                """
                INSERT INTO bookings (id, user_id, scheduled_trip_id, user_info_snapshot, total_price, currency, status, idempotency_key, payment_deadline, created_at)
                VALUES (?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, ?, ?)
                """,
                bookingId,
                userId,
                tripId,
                "{" + "\"fullName\":\"Nguyen Van A\","
                        + "\"email\":\"a@example.com\","
                        + "\"phone\":\"0900000000\","
                        + "\"dateOfBirth\":null,"
                        + "\"gender\":\"MALE\","
                        + "\"idDocumentNumber\":\"0123456789\","
                        + "\"addressLine\":\"123 Test Street\"}",
                450_000,
                "VND",
                "HELD",
                "idem-" + bookingId,
                Timestamp.from(paymentDeadline),
                Timestamp.from(CREATED_AT));
    }

    private void insertSeatAvailability() {
        insertSeatAvailability(SEAT_A2_ID, "HELD", BOOKING_ID, 225_000L);
        insertSeatAvailability(SEAT_A1_ID, "HELD", BOOKING_ID, 225_000L);
        insertSeatAvailability(SEAT_B1_ID, "HELD", BOOKING_ID, 225_000L);
        insertSeatAvailability(DELETED_SEAT_ID, "HELD", BOOKING_ID, 225_000L);
        insertSeatAvailability(DELETED_COACH_SEAT_ID, "HELD", BOOKING_ID, 225_000L);
        insertSeatAvailability(HELD_SEAT_ID, "HELD", BOOKING_ID, 225_000L);
        insertSeatAvailability(EXPIRED_HELD_SEAT_ID, "HELD", EXPIRED_BOOKING_ID, 225_000L);
        insertSeatAvailability(AVAILABLE_SEAT_ID, "AVAILABLE", null, null);
    }

    private void insertSeatAvailability(
            UUID seatId, String status, UUID bookingId, Long priceAtBooking) {
        jdbcTemplate.update("""
                INSERT INTO trip_seat_availability (scheduled_trip_id, seat_id, status, booking_id, price_at_booking, version)
                VALUES (?, ?, ?, ?, ?, ?)
                """, TRIP_ID, seatId, status, bookingId, priceAtBooking, 1);
    }
}
