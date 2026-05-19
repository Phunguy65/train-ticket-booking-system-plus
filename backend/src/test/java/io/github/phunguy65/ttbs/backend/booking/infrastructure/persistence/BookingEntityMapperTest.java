package io.github.phunguy65.ttbs.backend.booking.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BookingEntityMapperTest {

    private final BookingEntityMapper mapper = new BookingEntityMapper();

    @Test
    void toSummaryFailsFastWhenUserInfoSnapshotIsMissing() {
        BookingEntity entity = new BookingEntity();
        entity.setId(UUID.fromString("17171717-1717-1717-1717-171717171717"));
        entity.setUserId(UUID.fromString("18181818-1818-1818-1818-181818181818"));
        entity.setScheduledTripId(UUID.fromString("19191919-1919-1919-1919-191919191919"));
        entity.setTotalPrice(450000);
        entity.setCurrency("VND");
        entity.setStatus("CONFIRMED");
        entity.setPaymentDeadline(Instant.parse("2026-04-03T10:00:00Z"));
        entity.setCreatedAt(Instant.parse("2026-04-01T08:00:00Z"));

        assertThatThrownBy(() -> mapper.toSummary(entity))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Booking user info snapshot must not be null");
    }

    @Test
    void toSummaryMapsSnapshotFieldsIntoDomainProjection() {
        BookingEntity entity = new BookingEntity();
        entity.setId(UUID.fromString("20202020-2020-2020-2020-202020202020"));
        entity.setUserId(UUID.fromString("21212121-2121-2121-2121-212121212121"));
        entity.setScheduledTripId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        entity.setUserInfoSnapshot(new BookingUserInfoSnapshotJson(
                "Mapper User",
                "mapper@example.com",
                "0900000000",
                null,
                "MALE",
                "0123456789",
                "123 Test Street"));
        entity.setTotalPrice(450000);
        entity.setCurrency("VND");
        entity.setStatus("CONFIRMED");
        entity.setPaymentDeadline(Instant.parse("2026-04-03T10:00:00Z"));
        entity.setCreatedAt(Instant.parse("2026-04-01T08:00:00Z"));

        var summary = mapper.toSummary(entity);

        assertThat(summary.id()).isEqualTo(entity.getId());
        assertThat(summary.bookerInfo().fullName()).isEqualTo("Mapper User");
        assertThat(summary.bookerInfo().email()).isEqualTo("mapper@example.com");
        assertThat(summary.status()).isEqualTo("CONFIRMED");
    }
}
