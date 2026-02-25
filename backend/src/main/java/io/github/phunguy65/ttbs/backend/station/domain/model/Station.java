package io.github.phunguy65.ttbs.backend.station.domain.model;

import io.github.phunguy65.ttbs.backend.shared.domain.AggregateRoot;
import io.github.phunguy65.ttbs.backend.station.domain.event.StationCreated;
import java.time.Instant;

public class Station extends AggregateRoot<StationId> {

    private final StationId id;
    private final String code;
    private final String name;
    private final String city;
    private final Instant createdAt;

    private Station(StationId id, String code, String name, String city, Instant createdAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.city = city;
        this.createdAt = createdAt;
    }

    /**
     * Factory method for creating a new station. Registers {@link StationCreated} domain event.
     */
    public static Station create(StationId id, String code, String name, String city) {
        Instant now = Instant.now();
        Station station = new Station(id, code, name, city, now);
        station.registerEvent(StationCreated.of(id, code, name, city));
        return station;
    }

    /**
     * Factory method for reconstituting a station from persistence.
     * Does NOT register domain events.
     */
    public static Station reconstitute(
            StationId id, String code, String name, String city, Instant createdAt) {
        return new Station(id, code, name, city, createdAt);
    }

    @Override
    public StationId getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
