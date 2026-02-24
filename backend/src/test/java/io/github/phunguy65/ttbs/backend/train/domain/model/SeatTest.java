package io.github.phunguy65.ttbs.backend.train.domain.model;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SeatTest {

    private static final SeatId SEAT_ID = SeatId.of(UUID.randomUUID());
    private static final TrainId TRAIN_ID = TrainId.of(UUID.randomUUID());
    private static final String SEAT_NUMBER = "1A";
    private static final SeatClass SEAT_CLASS = SeatClass.ECONOMY;

    @Test
    void create_shouldSetAllFieldsCorrectly() {
        Seat seat = Seat.create(SEAT_ID, TRAIN_ID, SEAT_NUMBER, SEAT_CLASS);

        assertThat(seat.getId()).isEqualTo(SEAT_ID);
        assertThat(seat.getTrainId()).isEqualTo(TRAIN_ID);
        assertThat(seat.getSeatNumber()).isEqualTo(SEAT_NUMBER);
        assertThat(seat.getSeatClass()).isEqualTo(SEAT_CLASS);
        assertThat(seat.getCreatedAt()).isNotNull();
    }

    @Test
    void create_shouldNotEmitDomainEvents() {
        Seat seat = Seat.create(SEAT_ID, TRAIN_ID, SEAT_NUMBER, SEAT_CLASS);

        assertThat(seat.getDomainEvents()).isEmpty();
    }

    @Test
    void reconstitute_shouldRestoreAllFields() {
        Instant createdAt = Instant.parse("2024-06-01T10:00:00Z");

        Seat seat = Seat.reconstitute(SEAT_ID, TRAIN_ID, SEAT_NUMBER, SEAT_CLASS, createdAt);

        assertThat(seat.getId()).isEqualTo(SEAT_ID);
        assertThat(seat.getTrainId()).isEqualTo(TRAIN_ID);
        assertThat(seat.getSeatNumber()).isEqualTo(SEAT_NUMBER);
        assertThat(seat.getSeatClass()).isEqualTo(SEAT_CLASS);
        assertThat(seat.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void reconstitute_shouldNotEmitDomainEvents() {
        Instant createdAt = Instant.now();

        Seat seat = Seat.reconstitute(SEAT_ID, TRAIN_ID, SEAT_NUMBER, SEAT_CLASS, createdAt);

        assertThat(seat.getDomainEvents()).isEmpty();
    }

    @Test
    void seatClass_shouldHaveExpectedValues() {
        assertThat(SeatClass.values())
                .containsExactlyInAnyOrder(
                        SeatClass.ECONOMY, SeatClass.BUSINESS, SeatClass.FIRST_CLASS);
    }
}
