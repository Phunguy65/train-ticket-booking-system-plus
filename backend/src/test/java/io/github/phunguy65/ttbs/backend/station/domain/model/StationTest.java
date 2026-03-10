package io.github.phunguy65.ttbs.backend.station.domain.model;

import static org.assertj.core.api.Assertions.*;

import io.github.phunguy65.ttbs.backend.station.domain.event.StationCreated;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StationTest {

    private static final StationId STATION_ID = StationId.of(UUID.randomUUID());
    private static final String CODE = "HN";
    private static final String NAME = "Hanoi Station";
    private static final String CITY = "Hanoi";

    @Test
    void create_shouldRegisterExactlyOneStationCreatedEvent() {
        Station station = Station.create(STATION_ID, CODE, NAME, CITY);

        assertThat(station.getDomainEvents()).hasSize(1);
        assertThat(station.getDomainEvents().getFirst()).isInstanceOf(StationCreated.class);
    }

    @Test
    void create_stationCreatedEvent_shouldContainCorrectData() {
        Station station = Station.create(STATION_ID, CODE, NAME, CITY);

        StationCreated event = (StationCreated) station.getDomainEvents().getFirst();
        assertThat(event.stationId()).isEqualTo(STATION_ID);
        assertThat(event.code()).isEqualTo(CODE);
        assertThat(event.name()).isEqualTo(NAME);
        assertThat(event.city()).isEqualTo(CITY);
        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    void create_shouldSetCorrectFields() {
        Station station = Station.create(STATION_ID, CODE, NAME, CITY);

        assertThat(station.getId()).isEqualTo(STATION_ID);
        assertThat(station.getCode()).isEqualTo(CODE);
        assertThat(station.getName()).isEqualTo(NAME);
        assertThat(station.getCity()).isEqualTo(CITY);
        assertThat(station.getCreatedAt()).isNotNull();
    }

    @Test
    void reconstitute_shouldNotRegisterDomainEvents() {
        Instant createdAt = Instant.parse("2024-01-15T10:00:00Z");

        Station station = Station.reconstitute(STATION_ID, CODE, NAME, CITY, createdAt, null);

        assertThat(station.getDomainEvents()).isEmpty();
    }

    @Test
    void reconstitute_shouldRestoreAllFields() {
        Instant createdAt = Instant.parse("2024-01-15T10:00:00Z");

        Station station = Station.reconstitute(STATION_ID, CODE, NAME, CITY, createdAt, null);

        assertThat(station.getId()).isEqualTo(STATION_ID);
        assertThat(station.getCode()).isEqualTo(CODE);
        assertThat(station.getName()).isEqualTo(NAME);
        assertThat(station.getCity()).isEqualTo(CITY);
        assertThat(station.getCreatedAt()).isEqualTo(createdAt);
    }
}
