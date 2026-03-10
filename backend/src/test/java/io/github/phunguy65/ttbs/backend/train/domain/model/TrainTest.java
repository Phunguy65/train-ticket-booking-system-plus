package io.github.phunguy65.ttbs.backend.train.domain.model;

import static org.assertj.core.api.Assertions.*;

import io.github.phunguy65.ttbs.backend.train.domain.event.TrainCreated;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TrainTest {

    private static final TrainId TRAIN_ID = TrainId.of(UUID.randomUUID());
    private static final String TRAIN_NUMBER = "SE001";
    private static final String NAME = "Reunification Express";
    private static final int TOTAL_SEATS = 250;

    @Test
    void create_shouldRegisterExactlyOneTrainCreatedEvent() {
        Train train = Train.create(TRAIN_ID, TRAIN_NUMBER, NAME, TOTAL_SEATS);

        assertThat(train.getDomainEvents()).hasSize(1);
        assertThat(train.getDomainEvents().getFirst()).isInstanceOf(TrainCreated.class);
    }

    @Test
    void create_trainCreatedEvent_shouldContainCorrectData() {
        Train train = Train.create(TRAIN_ID, TRAIN_NUMBER, NAME, TOTAL_SEATS);

        TrainCreated event = (TrainCreated) train.getDomainEvents().getFirst();
        assertThat(event.trainId()).isEqualTo(TRAIN_ID);
        assertThat(event.trainNumber()).isEqualTo(TRAIN_NUMBER);
        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    void create_shouldSetCorrectFields() {
        Train train = Train.create(TRAIN_ID, TRAIN_NUMBER, NAME, TOTAL_SEATS);

        assertThat(train.getId()).isEqualTo(TRAIN_ID);
        assertThat(train.getTrainNumber()).isEqualTo(TRAIN_NUMBER);
        assertThat(train.getName()).isEqualTo(NAME);
        assertThat(train.getTotalSeats()).isEqualTo(TOTAL_SEATS);
        assertThat(train.getCreatedAt()).isNotNull();
    }

    @Test
    void reconstitute_shouldNotRegisterDomainEvents() {
        Instant createdAt = Instant.parse("2024-01-15T10:00:00Z");

        Train train =
                Train.reconstitute(TRAIN_ID, TRAIN_NUMBER, NAME, TOTAL_SEATS, createdAt, null);

        assertThat(train.getDomainEvents()).isEmpty();
    }

    @Test
    void reconstitute_shouldRestoreAllFields() {
        Instant createdAt = Instant.parse("2024-01-15T10:00:00Z");

        Train train =
                Train.reconstitute(TRAIN_ID, TRAIN_NUMBER, NAME, TOTAL_SEATS, createdAt, null);

        assertThat(train.getId()).isEqualTo(TRAIN_ID);
        assertThat(train.getTrainNumber()).isEqualTo(TRAIN_NUMBER);
        assertThat(train.getName()).isEqualTo(NAME);
        assertThat(train.getTotalSeats()).isEqualTo(TOTAL_SEATS);
        assertThat(train.getCreatedAt()).isEqualTo(createdAt);
    }
}
