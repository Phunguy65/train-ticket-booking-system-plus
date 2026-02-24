package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "route_seat_availability")
class RouteSeatAvailabilityEntity {

    @EmbeddedId
    private RouteSeatAvailabilityId id;

    @Column(name = "status", nullable = false)
    private String status;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    protected RouteSeatAvailabilityEntity() {}

    RouteSeatAvailabilityId getId() {
        return id;
    }

    void setId(RouteSeatAvailabilityId id) {
        this.id = id;
    }

    String getStatus() {
        return status;
    }

    void setStatus(String status) {
        this.status = status;
    }

    int getVersion() {
        return version;
    }

    void setVersion(int version) {
        this.version = version;
    }
}
