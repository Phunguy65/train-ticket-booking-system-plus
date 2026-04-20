package io.github.phunguy65.ttbs.backend.booking.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.phunguy65.ttbs.backend.TestContainerConfiguration;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.booking.domain.projection.BookingSummary;
import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainerConfiguration.class)
@Transactional
class BookingRepositoryAdapterTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_USER_ID =
            UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID SCHEDULED_TRIP_ID_1 =
            UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final UUID SCHEDULED_TRIP_ID_2 =
            UUID.fromString("40000000-0000-0000-0000-000000000002");
    private static final UUID SCHEDULED_TRIP_ID_3 =
            UUID.fromString("40000000-0000-0000-0000-000000000003");

    @Autowired
    private BookingRepositoryAdapter repository;

    @Autowired
    private BookingJpaRepository jpaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void findSummaryByIdReturnsProjectedUserDetails() {
        insertUser(USER_ID, "booking-summary@example.com");
        BookingEntity entity = bookingEntity(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                USER_ID,
                SCHEDULED_TRIP_ID_1,
                "CONFIRMED",
                Instant.parse("2026-04-02T10:00:00Z"),
                Instant.parse("2026-04-01T09:00:00Z"),
                "Nguyen Van A",
                "a@example.com");
        jpaRepository.save(entity);

        var summary = repository.findSummaryById(BookingId.of(entity.getId()));

        assertThat(summary).isPresent();
        assertThat(summary.orElseThrow().id()).isEqualTo(entity.getId());
        assertThat(summary.orElseThrow().userId()).isEqualTo(USER_ID);
        assertThat(summary.orElseThrow().scheduledTripId()).isEqualTo(entity.getScheduledTripId());
        assertThat(summary.orElseThrow().status()).isEqualTo("CONFIRMED");
        assertThat(summary.orElseThrow().bookerInfo().fullName()).isEqualTo("Nguyen Van A");
        assertThat(summary.orElseThrow().bookerInfo().email()).isEqualTo("a@example.com");
        assertThat(summary.orElseThrow().bookerInfo().phone()).isEqualTo("0900000000");
        assertThat(summary.orElseThrow().bookerInfo().dateOfBirth()).isNull();
    }

    @Test
    void findSummaryByIdReturnsEmptyWhenBookingDoesNotExist() {
        assertThat(repository.findSummaryById(
                        BookingId.of(UUID.fromString("00000000-0000-0000-0000-000000000000"))))
                .isEmpty();
    }

    @Test
    void findByUserIdAppliesDynamicSortAndFiltersOtherUsers() {
        insertUser(USER_ID, "booking-sort@example.com");
        insertUser(OTHER_USER_ID, "other-user@example.com");
        BookingEntity oldest = bookingEntity(
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                USER_ID,
                SCHEDULED_TRIP_ID_1,
                "HELD",
                Instant.parse("2026-04-03T10:00:00Z"),
                Instant.parse("2026-04-01T08:00:00Z"),
                "Oldest",
                "oldest@example.com");
        BookingEntity newest = bookingEntity(
                UUID.fromString("66666666-6666-6666-6666-666666666666"),
                USER_ID,
                SCHEDULED_TRIP_ID_2,
                "CONFIRMED",
                Instant.parse("2026-04-04T10:00:00Z"),
                Instant.parse("2026-04-02T08:00:00Z"),
                "Newest",
                "newest@example.com");
        BookingEntity otherUser = bookingEntity(
                UUID.fromString("88888888-8888-8888-8888-888888888888"),
                OTHER_USER_ID,
                SCHEDULED_TRIP_ID_3,
                "CANCELLED",
                Instant.parse("2026-04-05T10:00:00Z"),
                Instant.parse("2026-04-03T08:00:00Z"),
                "Other User",
                "other@example.com");
        jpaRepository.saveAll(List.of(oldest, newest, otherUser));

        PageResponse<BookingSummary> page = repository.findByUserId(
                UserId.of(USER_ID),
                0,
                10,
                List.of(SortOrder.desc("createdAt"), SortOrder.desc("id")));

        assertThat(page.content())
                .extracting(BookingSummary::id)
                .containsExactly(newest.getId(), oldest.getId());
        assertThat(page.total()).isEqualTo(2);
        assertThat(page.hasNext()).isFalse();
        assertThat(page.hasPrevious()).isFalse();
    }

    @Test
    void findByUserIdReturnsPaginationMetadataForLaterPages() {
        insertUser(USER_ID, "booking-page@example.com");
        BookingEntity first = bookingEntity(
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                USER_ID,
                SCHEDULED_TRIP_ID_1,
                "HELD",
                Instant.parse("2026-04-03T10:00:00Z"),
                Instant.parse("2026-04-01T08:00:00Z"),
                "First",
                "first@example.com");
        BookingEntity second = bookingEntity(
                UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"),
                USER_ID,
                SCHEDULED_TRIP_ID_2,
                "CONFIRMED",
                Instant.parse("2026-04-04T10:00:00Z"),
                Instant.parse("2026-04-02T08:00:00Z"),
                "Second",
                "second@example.com");
        BookingEntity third = bookingEntity(
                UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"),
                USER_ID,
                SCHEDULED_TRIP_ID_3,
                "CANCELLED",
                Instant.parse("2026-04-05T10:00:00Z"),
                Instant.parse("2026-04-03T08:00:00Z"),
                "Third",
                "third@example.com");
        jpaRepository.saveAll(List.of(first, second, third));

        var page = repository.findByUserId(
                UserId.of(USER_ID),
                1,
                2,
                List.of(SortOrder.desc("createdAt"), SortOrder.desc("id")));

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().getFirst().id()).isEqualTo(first.getId());
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(2);
        assertThat(page.total()).isEqualTo(3);
        assertThat(page.hasNext()).isFalse();
        assertThat(page.hasPrevious()).isTrue();
    }

    @Test
    void findByUserIdSupportsAscendingSortOrder() {
        insertUser(USER_ID, "booking-sort-asc@example.com");
        BookingEntity older = bookingEntity(
                UUID.fromString("13131313-1313-1313-1313-131313131313"),
                USER_ID,
                SCHEDULED_TRIP_ID_1,
                "CONFIRMED",
                Instant.parse("2026-04-03T10:00:00Z"),
                Instant.parse("2026-04-01T08:00:00Z"),
                "Older",
                "older@example.com");
        BookingEntity newer = bookingEntity(
                UUID.fromString("14141414-1414-1414-1414-141414141414"),
                USER_ID,
                SCHEDULED_TRIP_ID_2,
                "CONFIRMED",
                Instant.parse("2026-04-04T10:00:00Z"),
                Instant.parse("2026-04-02T08:00:00Z"),
                "Newer",
                "newer@example.com");
        jpaRepository.saveAll(List.of(older, newer));

        PageResponse<BookingSummary> page = repository.findByUserId(
                UserId.of(USER_ID), 0, 10, List.of(SortOrder.asc("createdAt")));

        assertThat(page.content())
                .extracting(BookingSummary::id)
                .containsExactly(older.getId(), newer.getId());
    }

    @Test
    void findByUserIdHandlesEmptySortListAndExactPageSizeBoundary() {
        insertUser(USER_ID, "booking-empty-sort@example.com");
        BookingEntity first = bookingEntity(
                UUID.fromString("15151515-1515-1515-1515-151515151515"),
                USER_ID,
                SCHEDULED_TRIP_ID_1,
                "CONFIRMED",
                Instant.parse("2026-04-03T10:00:00Z"),
                Instant.parse("2026-04-01T08:00:00Z"),
                "First",
                "first-empty@example.com");
        BookingEntity second = bookingEntity(
                UUID.fromString("16161616-1616-1616-1616-161616161616"),
                USER_ID,
                SCHEDULED_TRIP_ID_2,
                "CONFIRMED",
                Instant.parse("2026-04-04T10:00:00Z"),
                Instant.parse("2026-04-02T08:00:00Z"),
                "Second",
                "second-empty@example.com");
        jpaRepository.saveAll(List.of(first, second));

        PageResponse<BookingSummary> page =
                repository.findByUserId(UserId.of(USER_ID), 0, 2, List.of());

        assertThat(page.content()).hasSize(2);
        assertThat(page.total()).isEqualTo(2);
        assertThat(page.hasNext()).isFalse();
        assertThat(page.hasPrevious()).isFalse();
    }

    private BookingEntity bookingEntity(
            UUID bookingId,
            UUID userId,
            UUID scheduledTripId,
            String status,
            Instant paymentDeadline,
            Instant createdAt,
            String fullName,
            String email) {
        BookingEntity entity = new BookingEntity();
        entity.setId(bookingId);
        entity.setUserId(userId);
        entity.setScheduledTripId(scheduledTripId);
        entity.setUserInfoSnapshot(new BookingUserInfoSnapshotJson(
                fullName, email, "0900000000", null, "MALE", "0123456789", "123 Test Street"));
        entity.setTotalPrice(450000);
        entity.setCurrency("VND");
        entity.setStatus(status);
        entity.setIdempotencyKey("idem-" + bookingId);
        entity.setPaymentDeadline(paymentDeadline);
        entity.setCreatedAt(createdAt);
        return entity;
    }

    private void insertUser(UUID userId, String email) {
        jdbcTemplate.update(
                """
                INSERT INTO users (id, email, password_hash, full_name, role, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                userId,
                email,
                "test-password-hash",
                "Test User",
                "CUSTOMER",
                Timestamp.from(Instant.parse("2026-04-01T00:00:00Z")),
                Timestamp.from(Instant.parse("2026-04-01T00:00:00Z")));
    }
}
