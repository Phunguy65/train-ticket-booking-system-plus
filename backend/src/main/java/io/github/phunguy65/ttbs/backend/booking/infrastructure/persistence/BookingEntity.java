package io.github.phunguy65.ttbs.backend.booking.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bookings")
class BookingEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "route_id", nullable = false, updatable = false)
    private UUID routeId;

    @Column(name = "passenger_name", nullable = false, length = 255)
    private String passengerName;

    @Column(name = "passenger_email", nullable = false, length = 255)
    private String passengerEmail;

    @Column(name = "passenger_phone", length = 20)
    private String passengerPhone;

    @Column(name = "total_price", nullable = false)
    private long totalPrice;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "idempotency_key", length = 255)
    private String idempotencyKey;

    @Column(name = "payment_deadline")
    private Instant paymentDeadline;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected BookingEntity() {}

    UUID getId() {
        return id;
    }

    void setId(UUID id) {
        this.id = id;
    }

    UUID getUserId() {
        return userId;
    }

    void setUserId(UUID userId) {
        this.userId = userId;
    }

    UUID getRouteId() {
        return routeId;
    }

    void setRouteId(UUID routeId) {
        this.routeId = routeId;
    }

    String getPassengerName() {
        return passengerName;
    }

    void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    String getPassengerEmail() {
        return passengerEmail;
    }

    void setPassengerEmail(String passengerEmail) {
        this.passengerEmail = passengerEmail;
    }

    String getPassengerPhone() {
        return passengerPhone;
    }

    void setPassengerPhone(String passengerPhone) {
        this.passengerPhone = passengerPhone;
    }

    long getTotalPrice() {
        return totalPrice;
    }

    void setTotalPrice(long totalPrice) {
        this.totalPrice = totalPrice;
    }

    String getCurrency() {
        return currency;
    }

    void setCurrency(String currency) {
        this.currency = currency;
    }

    String getStatus() {
        return status;
    }

    void setStatus(String status) {
        this.status = status;
    }

    String getIdempotencyKey() {
        return idempotencyKey;
    }

    void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    Instant getPaymentDeadline() {
        return paymentDeadline;
    }

    void setPaymentDeadline(Instant paymentDeadline) {
        this.paymentDeadline = paymentDeadline;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
