package io.github.phunguy65.ttbs.backend.station.domain.model;

import io.github.phunguy65.ttbs.backend.shared.domain.AggregateRoot;
import io.github.phunguy65.ttbs.backend.station.domain.event.StationCreated;
import io.github.phunguy65.ttbs.backend.station.domain.event.StationDeleted;
import io.github.phunguy65.ttbs.backend.station.domain.event.StationUpdated;
import java.time.Instant;

public class Station extends AggregateRoot<StationId> {

    private final StationId id;
    private final StationCode code;
    private final String name;
    private final String city;
    private final Instant createdAt;
    private Instant deletedAt;

    private Station(
            StationId id,
            String code,
            String name,
            String city,
            Instant createdAt,
            Instant deletedAt) {
        this.id = id;
        this.code = StationCode.of(code);
        this.name = name;
        this.city = city;
        this.createdAt = createdAt;
        this.deletedAt = deletedAt;
    }

    /**
     * Factory method for creating a new station. Registers {@link StationCreated} domain event.
     */
    public static Station create(StationId id, String code, String name, String city) {
        Instant now = Instant.now();
        Station station = new Station(id, code, name, city, now, null);
        station.registerEvent(StationCreated.of(id, code, name, city));
        return station;
    }

    /**
     * Factory method for reconstituting a station from persistence.
     * Does NOT register domain events.
     */
    public static Station reconstitute(
            StationId id,
            String code,
            String name,
            String city,
            Instant createdAt,
            Instant deletedAt) {
        return new Station(id, code, name, city, createdAt, deletedAt);
    }

    /**
     * Updates this station's business fields and registers a {@link StationUpdated} domain event.
     * Returns a new {@code Station} instance (fields are immutable).
     */
    public Station update(String code, String name, String city) {
        Station updated = new Station(this.id, code, name, city, this.createdAt, this.deletedAt);
        updated.registerEvent(StationUpdated.of(this.id, code, name, city));
        return updated;
    }

    /**
     * Soft-deletes this station by setting {@code deletedAt} to now and registering a
     * {@link StationDeleted} domain event. Idempotent: if already deleted, returns immediately.
     */
    public void softDelete() {
        if (isDeleted()) {
            return;
        }
        this.deletedAt = Instant.now();
        registerEvent(StationDeleted.of(id));
    }

    /** Returns {@code true} if this station has been soft-deleted. */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    @Override
    public StationId getId() {
        return id;
    }

    public String getCode() {
        return code.value();
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

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
