package io.github.phunguy65.ttbs.backend.booking.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "bookings")
class BookingEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "scheduled_trip_id", nullable = false, updatable = false)
    private UUID scheduledTripId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "user_info_snapshot", nullable = false, columnDefinition = "jsonb")
    private BookingUserInfoSnapshotJson userInfoSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "passengers_snapshot", columnDefinition = "jsonb")
    private List<BookingPassengerSnapshotJson> passengersSnapshot;

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

    UUID getScheduledTripId() {
        return scheduledTripId;
    }

    void setScheduledTripId(UUID scheduledTripId) {
        this.scheduledTripId = scheduledTripId;
    }

    BookingUserInfoSnapshotJson getUserInfoSnapshot() {
        return userInfoSnapshot;
    }

    void setUserInfoSnapshot(BookingUserInfoSnapshotJson userInfoSnapshot) {
        this.userInfoSnapshot = userInfoSnapshot;
    }

    List<BookingPassengerSnapshotJson> getPassengersSnapshot() {
        return passengersSnapshot;
    }

    void setPassengersSnapshot(List<BookingPassengerSnapshotJson> passengersSnapshot) {
        this.passengersSnapshot = passengersSnapshot;
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
