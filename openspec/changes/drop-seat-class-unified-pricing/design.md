# Context

The `train` module currently models seat pricing via a `SeatClass` enum (ECONOMY, BUSINESS, FIRST_CLASS) with price multipliers (×1.0, ×1.5, ×2.0). `PricingService` computes `unitPrice = route.basePrice × seatClass.getPriceMultiplier()` and snapshots both `price_at_booking` and `seat_class_at_booking` into the `booking_seats` table.

This multiplier mechanism adds domain complexity — a separate enum, two extra columns across two tables, and 24+ files carrying the `seatClass` concept — with no current business need for tiered pricing. The project is in active development (both frontends are skeletons), so the cost of this breaking change is low.

## Goals / Non-Goals

**Goals:**

- Remove `SeatClass` enum from the domain entirely
- Simplify `PricingService` so `unitPrice = route.basePrice` (no multiplier)
- Drop `seat_class` from `seats` table and `seat_class_at_booking` from `booking_seats` table via a single Flyway migration
- Remove `seatClass` from all API request/response contracts and the OpenAPI spec
- Keep `price_at_booking` in `booking_seats` intact — it retains the price snapshot per seat per booking

**Non-Goals:**

- Introducing any new pricing model (e.g., per-seat fixed price, surge pricing) — `route.basePrice` is the only price source after this change
- API versioning (v2 endpoint) — breaking change applied directly to v1 since frontends are not yet consuming `seatClass`
- Migrating or backfilling existing `seat_class_at_booking` data — the column is dropped, historical class labels are not preserved

## Decisions

### Decision 1: Delete `SeatClass` entirely rather than keeping it as a non-pricing attribute

**Chosen:** Delete the enum and all references.

**Alternatives considered:**
- Keep `SeatClass` on `Seat` as a physical/layout attribute (e.g., economy section vs. business section) but strip it from pricing. Rejected because there is no current requirement to display or filter by seat layout class. Adding it back later when there is a real requirement is cheaper than carrying dead code now.

**Rationale:** YAGNI — no active feature depends on seat classification. Every file that touches `SeatClass` becomes simpler after deletion.

---

### Decision 2: Drop `seat_class_at_booking` column (no nullable fallback)

**Chosen:** Drop the column outright in the migration.

**Alternatives considered:**
- Make column `NULLABLE` to preserve historical rows and only omit it for new bookings. Rejected because the system has no production data; nullable orphan columns introduce schema ambiguity.

**Rationale:** Clean schema is worth more than preserving column shape for data that was never in production. `price_at_booking` still provides the price snapshot — the class label is not needed for any query or report.

---

### Decision 3: Single Flyway migration file drops both columns atomically

**Chosen:** New migration `V8.0.0__drop_seat_class.sql` drops both columns in one transaction.

```sql
ALTER TABLE seats DROP COLUMN seat_class;
ALTER TABLE booking_seats DROP COLUMN seat_class_at_booking;
```

**Rationale:** The two columns are semantically coupled (both represent the same removed concept). Dropping them together avoids an intermediate inconsistent state where the domain no longer uses `SeatClass` but the columns still exist.

---

### Decision 4: `PricingService.calculatePrices()` signature stays the same; only the body changes

**Chosen:** Keep `calculatePrices(Route route, List<Seat> seats)` signature; remove the multiplier line.

```java
// BEFORE
BigDecimal unitPrice = basePrice
    .multiply(seat.getSeatClass().getPriceMultiplier())
    .setScale(2, RoundingMode.HALF_UP);
return BookedSeat.of(seat.getId(), unitPrice, seat.getSeatClass());

// AFTER
BigDecimal unitPrice = basePrice.setScale(2, RoundingMode.HALF_UP);
return BookedSeat.of(seat.getId(), unitPrice);
```

**Rationale:** `Route` is still needed to provide `basePrice`; `List<Seat>` is still needed to know which seats are being held. Removing the `Seat` parameter would require refactoring the hold flow unnecessarily.

---

### Decision 5: `BookedSeat` value object drops `seatClass` field

**Chosen:** `BookedSeat` becomes a two-field record: `SeatId seatId` + `BigDecimal unitPrice`.

**Rationale:** `BookedSeat` is a price snapshot value object. Without `seatClass`, its only remaining purpose is to associate a seat ID with its locked price. This is the minimal correct shape.

---

### Decision 6: `UpdateSeatUseCase` / `PATCH /seats/{id}` stays — but only updates `seatNumber`

**Chosen:** Keep the endpoint; remove `seatClass` from the request body. The endpoint still serves the purpose of correcting a seat number.

**Rationale:** Deleting the endpoint is a larger scope change. The request DTO simplifies from `JsonNullable<SeatClass> seatClass` (patch field) to only `seatNumber`.

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| OpenAPI spec diverges from implementation | Update `openapi.yaml` in the same PR; `SeatControllerTest` and `BookingControllerTest` cover the actual JSON shapes |
| `PricingServiceTest` tests three different multiplier outcomes — all become equal after this change | Replace multiplier-differentiated assertions with flat `basePrice` assertions; coverage is maintained |
| Future need for tiered pricing requires re-introducing a similar concept | Acceptable — the design is simpler to extend from a clean base than to maintain a half-used abstraction |
| Flyway migration irreversible in production | Acceptable for current stage (no production deployment) |

## Migration Plan

1. Apply `V8.0.0__drop_seat_class.sql` migration (drops two columns)
2. Code changes compile — no runtime state carries the old columns
3. Tests pass with updated fixtures
4. OpenAPI spec updated and committed alongside code changes
5. No rollback strategy required — project has no production database

## Open Questions

- None — all decisions finalized based on exploration session.
