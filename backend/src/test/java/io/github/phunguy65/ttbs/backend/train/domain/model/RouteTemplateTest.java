package io.github.phunguy65.ttbs.backend.train.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RouteTemplateTest {

    @Test
    void createInitializesActiveTemplate() {
        RouteTemplateId id = RouteTemplateId.of(UUID.randomUUID());
        StationId origin = StationId.of(UUID.randomUUID());
        StationId destination = StationId.of(UUID.randomUUID());
        Money price = Money.vnd(120_000L);

        RouteTemplate template = RouteTemplate.create(id, origin, destination, price);

        assertThat(template.getId()).isEqualTo(id);
        assertThat(template.getOriginStationId()).isEqualTo(origin);
        assertThat(template.getDestinationStationId()).isEqualTo(destination);
        assertThat(template.getBasePrice()).isEqualTo(price);
        assertThat(template.getCreatedAt()).isNotNull();
        assertThat(template.getDeletedAt()).isNull();
        assertThat(template.isDeleted()).isFalse();
    }

    @Test
    void createRejectsNullOriginStationId() {
        assertThatThrownBy(() -> RouteTemplate.create(
                        RouteTemplateId.of(UUID.randomUUID()),
                        null,
                        StationId.of(UUID.randomUUID()),
                        Money.vnd(120_000L)))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("originStationId must not be null");
    }

    @Test
    void createRejectsNullDestinationStationId() {
        assertThatThrownBy(() -> RouteTemplate.create(
                        RouteTemplateId.of(UUID.randomUUID()),
                        StationId.of(UUID.randomUUID()),
                        null,
                        Money.vnd(120_000L)))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("destinationStationId must not be null");
    }

    @Test
    void createRejectsNullBasePrice() {
        assertThatThrownBy(() -> RouteTemplate.create(
                        RouteTemplateId.of(UUID.randomUUID()),
                        StationId.of(UUID.randomUUID()),
                        StationId.of(UUID.randomUUID()),
                        null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("basePrice must not be null");
    }

    @Test
    void updateReplacesStationsAndPrice() {
        RouteTemplate template = createTemplate();
        StationId newOrigin = StationId.of(UUID.randomUUID());
        StationId newDestination = StationId.of(UUID.randomUUID());
        Money newPrice = Money.vnd(180_000L);

        template.update(newOrigin, newDestination, newPrice);

        assertThat(template.getOriginStationId()).isEqualTo(newOrigin);
        assertThat(template.getDestinationStationId()).isEqualTo(newDestination);
        assertThat(template.getBasePrice()).isEqualTo(newPrice);
    }

    @Test
    void softDeleteIsIdempotent() {
        RouteTemplate template = createTemplate();

        template.softDelete();
        Instant firstDeletedAt = template.getDeletedAt();
        template.softDelete();

        assertThat(template.isDeleted()).isTrue();
        assertThat(template.getDeletedAt()).isEqualTo(firstDeletedAt);
    }

    @Test
    void reconstituteRestoresPersistedState() {
        RouteTemplateId id = RouteTemplateId.of(UUID.randomUUID());
        StationId origin = StationId.of(UUID.randomUUID());
        StationId destination = StationId.of(UUID.randomUUID());
        Money price = Money.vnd(150_000L);
        Instant createdAt = Instant.parse("2026-03-01T10:15:30Z");
        Instant deletedAt = Instant.parse("2026-03-02T10:15:30Z");

        RouteTemplate template =
                RouteTemplate.reconstitute(id, origin, destination, price, createdAt, deletedAt);

        assertThat(template.getId()).isEqualTo(id);
        assertThat(template.getOriginStationId()).isEqualTo(origin);
        assertThat(template.getDestinationStationId()).isEqualTo(destination);
        assertThat(template.getBasePrice()).isEqualTo(price);
        assertThat(template.getCreatedAt()).isEqualTo(createdAt);
        assertThat(template.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(template.isDeleted()).isTrue();
    }

    private static RouteTemplate createTemplate() {
        return RouteTemplate.create(
                RouteTemplateId.of(UUID.randomUUID()),
                StationId.of(UUID.randomUUID()),
                StationId.of(UUID.randomUUID()),
                Money.vnd(120_000L));
    }
}
