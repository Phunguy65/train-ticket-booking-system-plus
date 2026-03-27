package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "trip_seat_availability")
class RouteSeatAvailabilityEntity {

    @EmbeddedId
    private RouteSeatAvailabilityId id;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "booking_id")
    private java.util.UUID bookingId;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

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

    Integer getVersion() {
        return version;
    }

    void setVersion(Integer version) {
        this.version = version;
    }

    java.util.UUID getBookingId() {
        return bookingId;
    }

    void setBookingId(java.util.UUID bookingId) {
        this.bookingId = bookingId;
    }
}
