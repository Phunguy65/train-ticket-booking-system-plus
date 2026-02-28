package io.github.phunguy65.ttbs.backend.train.domain.model;

import static org.assertj.core.api.Assertions.*;

import io.github.phunguy65.ttbs.backend.train.domain.event.CoachDeleted;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CoachTest {

    private static final CoachId COACH_ID = CoachId.of(UUID.randomUUID());
    private static final TrainId TRAIN_ID = TrainId.of(UUID.randomUUID());
    private static final int CAR_NUMBER = 1;
    private static final int TOTAL_SEATS = 50;

    @Test
    void create_shouldSetAllFieldsCorrectly() {
        Coach coach = Coach.create(COACH_ID, TRAIN_ID, CAR_NUMBER, TOTAL_SEATS);

        assertThat(coach.getId()).isEqualTo(COACH_ID);
        assertThat(coach.getTrainId()).isEqualTo(TRAIN_ID);
        assertThat(coach.getCarNumber()).isEqualTo(CAR_NUMBER);
        assertThat(coach.getTotalSeats()).isEqualTo(TOTAL_SEATS);
        assertThat(coach.getCreatedAt()).isNotNull();
        assertThat(coach.getDeletedAt()).isNull();
    }

    @Test
    void create_shouldNotEmitDomainEvents() {
        Coach coach = Coach.create(COACH_ID, TRAIN_ID, CAR_NUMBER, TOTAL_SEATS);

        assertThat(coach.getDomainEvents()).isEmpty();
    }

    @Test
    void softDelete_shouldEmitCoachDeletedEvent() {
        Coach coach = Coach.create(COACH_ID, TRAIN_ID, CAR_NUMBER, TOTAL_SEATS);

        coach.softDelete();

        assertThat(coach.getDomainEvents()).hasSize(1);
        assertThat(coach.getDomainEvents().getFirst()).isInstanceOf(CoachDeleted.class);
        CoachDeleted event = (CoachDeleted) coach.getDomainEvents().getFirst();
        assertThat(event.coachId()).isEqualTo(COACH_ID);
        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    void softDelete_shouldSetDeletedAt() {
        Coach coach = Coach.create(COACH_ID, TRAIN_ID, CAR_NUMBER, TOTAL_SEATS);

        coach.softDelete();

        assertThat(coach.getDeletedAt()).isNotNull();
        assertThat(coach.isDeleted()).isTrue();
    }

    @Test
    void softDelete_isIdempotent() {
        Coach coach = Coach.create(COACH_ID, TRAIN_ID, CAR_NUMBER, TOTAL_SEATS);
        coach.softDelete();
        Instant firstDeletedAt = coach.getDeletedAt();

        // Call softDelete again — should not emit another event
        coach.softDelete();

        assertThat(coach.getDomainEvents()).hasSize(1);
        assertThat(coach.getDeletedAt()).isEqualTo(firstDeletedAt);
    }

    @Test
    void reconstitute_shouldRestoreAllFields() {
        Instant createdAt = Instant.parse("2024-06-01T10:00:00Z");
        Instant deletedAt = Instant.parse("2024-06-10T12:00:00Z");

        Coach coach = Coach.reconstitute(
                COACH_ID, TRAIN_ID, CAR_NUMBER, TOTAL_SEATS, createdAt, deletedAt);

        assertThat(coach.getId()).isEqualTo(COACH_ID);
        assertThat(coach.getTrainId()).isEqualTo(TRAIN_ID);
        assertThat(coach.getCarNumber()).isEqualTo(CAR_NUMBER);
        assertThat(coach.getTotalSeats()).isEqualTo(TOTAL_SEATS);
        assertThat(coach.getCreatedAt()).isEqualTo(createdAt);
        assertThat(coach.getDeletedAt()).isEqualTo(deletedAt);
    }

    @Test
    void reconstitute_shouldNotRegisterDomainEvents() {
        Instant createdAt = Instant.now();

        Coach coach =
                Coach.reconstitute(COACH_ID, TRAIN_ID, CAR_NUMBER, TOTAL_SEATS, createdAt, null);

        assertThat(coach.getDomainEvents()).isEmpty();
    }
}
