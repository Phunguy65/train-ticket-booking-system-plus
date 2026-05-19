# Context

The current booking implementation snapshots the authenticated user into
`BookingUserInfo` and uses that single snapshot as the passenger identity across
booking creation, booking detail, payment detail, and ticket presentation. That
model is no longer sufficient because the system already supports multi-seat
selection, but it cannot represent which traveler is assigned to which seat or
capture bookings where the authenticated user purchases seats for other people.

This change crosses backend domain, application, persistence, API contract,
database migration, generated frontend types, booking UI, payment UI, ticket UI,
and localization. It must preserve backward compatibility for existing bookings
that do not yet have passenger snapshots, fit the current Spring Boot clean
architecture and Next.js customer app patterns, and avoid introducing a second
source of truth for seat-limit rules beyond environment-backed configuration.

## Goals / Non-Goals

**Goals:**

- Separate the booking booker snapshot from traveler data by keeping
  `BookingUserInfo` as booker information and adding explicit per-seat passenger
  value objects.
- Enforce booking invariants during booking creation: configurable maximum seat
  count, one passenger per selected seat, valid seat-to-passenger assignment,
  and unique passenger ID document numbers within a booking.
- Persist passenger snapshots in PostgreSQL JSONB while allowing older rows with
  no passenger snapshot to continue loading.
- Expose the new structure consistently through booking detail, payment detail,
  and ticket-facing API responses so frontend views can render per-seat
  passengers.
- Update the customer booking flow to collect one passenger form per selected
  seat, perform duplicate-document validation before submit, and disable booking
  confirmation until the passenger payload is complete and valid.
- Keep backend and frontend seat limits configurable via env defaults of `5`.

**Non-Goals:**

- Changing the underlying seat hold/payment workflow or booking status machine.
- Introducing passenger profile persistence outside the booking aggregate; the
  passenger list remains a booking snapshot, not a reusable customer address
  book.
- Reusing the authenticated user's profile automatically as a passenger form.
- Backfilling historical bookings with passenger snapshots in this change.
- Changing unrelated account/profile management or admin booking flows.

## Decisions

### 1. Keep booker and passengers as separate booking snapshots inside the aggregate

`BookingUserInfo` will remain the authenticated-user snapshot but will be
renamed at aggregate/API boundaries to `bookerInfo`. A new `BookingPassenger`
value object will capture `seatId`, `fullName`, `idDocumentNumber`,
`dateOfBirth`, and `gender`, and the `Booking` aggregate will own a
`List<BookingPassenger>`.

- **Why this approach:** it preserves the current snapshot-based design while
  making the seat-to-traveler relationship explicit and keeping booking data
  self-contained for read models and ticket rendering.
- **Alternative considered:** introduce a shared passenger entity/table with
  booking relations. Rejected because requirements only need immutable
  per-booking traveler snapshots and a relational passenger model would add
  lifecycle and UI complexity without reuse requirements.

### 2. Enforce passenger invariants in the booking aggregate and create-booking use case

The dynamic seat-limit rule depends on environment configuration, so
`CreateBookingUseCase` will inject `BookingConfig` and validate maximum seat
count before aggregate creation. Structural booking invariants that do not
depend on infrastructure config—especially duplicate passenger ID document
numbers—will also be guarded when building the aggregate, ensuring
reconstitution and future callers cannot create invalid bookings silently.

- **Why this approach:** use-case validation is the right place for
  config-dependent request rules, while aggregate validation protects the domain
  model from invalid passenger collections regardless of entry point.
- **Alternative considered:** put all validation only in the web request DTO via
  annotations. Rejected because the max limit is dynamic, cross-item uniqueness
  and seat assignment checks are easier to express in application/domain logic,
  and domain invariants should not depend on HTTP validation.

### 3. Store passengers as JSONB snapshots alongside the existing booker snapshot

The `bookings` table will gain a nullable `passengers_snapshot JSONB` column,
represented by a new `BookingPassengerSnapshotJson` persistence record and
mapped through `BookingEntityMapper`. Existing `user_info_snapshot` storage
remains in place, with naming changes handled in Java/API layers rather than a
disruptive database column rename.

- **Why this approach:** it matches the current persistence pattern, minimizes
  migration scope, keeps read/write mapping straightforward, and allows legacy
  rows to remain valid by treating a null passenger snapshot as an empty or
  absent passenger list for backward-compatible reads.
- **Alternative considered:** normalize passengers into a dedicated booking
  passenger table. Rejected because seat-level passenger data is bounded by a
  small configured limit, queried only as part of booking reads, and better fits
  the existing snapshot-oriented aggregate persistence model.

### 4. Preserve public API compatibility by adding new fields and semantic renames at the contract layer

Booking and payment responses will expose `bookerInfo` plus `passengers`, where
each passenger includes its seat assignment. Existing frontend surfaces that
used the single passenger snapshot will be updated to render the new list. For
legacy bookings with no passenger snapshot, read use cases should still return a
compatible response shape; the preferred fallback is to preserve `bookerInfo`
and emit an empty passenger list rather than failing deserialization.

- **Why this approach:** API consumers need enough data to render new UI without
  breaking historical bookings, and explicit `bookerInfo` naming removes the
  current ambiguity between purchaser and traveler.
- **Alternative considered:** overload the existing `passengerInfo` field with
  mixed semantics. Rejected because it would keep the underlying ambiguity and
  make frontend migration and OpenAPI typing harder.

### 5. Model the booking confirmation UI as one passenger form per selected seat

The customer frontend will render a `PassengerList` that derives one form from
the selected seats and contains a `PassengerForm` for each seat. Validation will
run locally for required fields and duplicate `idDocumentNumber` values across
forms, and the confirm action will remain disabled until every selected seat has
exactly one valid passenger payload.

- **Why this approach:** it mirrors the backend invariant directly in the UI,
  reduces payload assembly mistakes, and makes seat-to-passenger assignment
  visually obvious before submission.
- **Alternative considered:** a free-form passenger list plus separate seat
  assignment picker. Rejected because it creates more interaction steps and
  makes mismatch errors more likely.

### 6. Centralize seat-limit configuration in typed backend/frontend config wrappers

Backend code will use a new `BookingConfig` component backed by
`booking.max-seats-per-booking`, while frontend code will read from a typed
`env.ts` wrapper using `NEXT_PUBLIC_MAX_SEATS_PER_BOOKING`. Shared utilities
such as `customer-utils.ts` and seat-selection logic will reference those
wrappers instead of hardcoded `5` values.

- **Why this approach:** it keeps configuration discoverable, testable, and
  consistent across modules while preserving safe defaults.
- **Alternative considered:** leave the frontend hardcoded and rely on backend
  rejection for mismatches. Rejected because it would produce avoidable UX
  inconsistencies and allow the customer UI to advertise invalid limits.

## Risks / Trade-offs

- **[Risk] Historical bookings have `null` passenger snapshots, but new UIs now
  expect a passenger list** → **Mitigation:** map missing snapshots to an empty
  list and ensure booking/payment/ticket UIs handle zero-passenger legacy data
  gracefully.
- **[Risk] Renaming API semantics from passenger/user info to `bookerInfo` can
  ripple through generated SDK types and multiple customer screens** →
  **Mitigation:** update the OpenAPI contract and regenerate types as part of
  the same change so frontend compilation catches all call sites.
- **[Risk] Duplicate-document validation could drift between frontend and
  backend** → **Mitigation:** keep backend validation authoritative and mirror
  it on the frontend only for early feedback using the same field name and error
  wording.
- **[Risk] Seat selection and booking confirmation may disagree on allowed seat
  counts if env values diverge between apps** → **Mitigation:** document both
  env variables with the same default and keep the frontend limit strictly as a
  UX hint while backend remains the final enforcement point.
- **[Risk] Introducing passenger-seat mapping into responses increases UI
  complexity on detail and ticket pages** → **Mitigation:** standardize response
  DTOs so every surface receives the same passenger shape including `seatId`,
  enabling straightforward rendering.

## Migration Plan

1. Add backend configuration support for `booking.max-seats-per-booking` and the
   domain/application model changes for `bookerInfo` plus passengers.
2. Add the nullable `passengers_snapshot` Flyway migration and persistence
   mapper updates so new bookings can store passenger data without breaking old
   rows.
3. Update booking, payment, and ticket response DTOs plus OpenAPI generation to
   expose `bookerInfo` and `passengers`.
4. Regenerate frontend API types from the updated contract.
5. Update customer env/config utilities, booking confirmation passenger forms,
   and downstream detail/payment/ticket views to consume the new response model.
6. Add locale messages and automated coverage for passenger validation,
   configurable limits, and legacy booking rendering.
7. Roll back, if necessary, by reverting application code first; the added
   nullable JSONB column can remain in place safely because old code ignores it.

## Open Questions

- The current ticket and detail pages should tolerate legacy bookings with no
  passengers, but the desired exact fallback presentation (empty state vs.
  derived single passenger from booker info) should be confirmed during
  implementation.
- The requirement specifies `gender` as a required string; implementation should
  confirm whether the frontend must constrain it to the same option set already
  used by user profile forms.
- If booking-level responses still expose both legacy and renamed fields during
  transition, implementation should confirm whether the OpenAPI contract intends
  a clean rename only or temporary dual-field compatibility.
