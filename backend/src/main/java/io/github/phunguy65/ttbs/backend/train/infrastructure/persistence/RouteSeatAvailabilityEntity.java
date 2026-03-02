package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "route_seat_availability")
class RouteSeatAvailabilityEntity {

    @EmbeddedId
    private RouteSeatAvailabilityId id;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

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
}
