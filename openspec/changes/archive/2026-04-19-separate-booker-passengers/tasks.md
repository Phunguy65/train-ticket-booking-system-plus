# Tasks

## 1. Backend booking configuration and domain model

- [x] 1.1 Create `backend/.../booking/infrastructure/.../BookingConfig` with
      `@Value("${booking.max-seats-per-booking:5}")` and expose a getter for the
      configured seat limit
- [x] 1.2 Update `backend/src/main/resources/application.yaml` to define
      `booking.max-seats-per-booking: ${BOOKING_MAX_SEATS:5}` under the existing
      `booking:` section
- [x] 1.3 Create `BookingPassenger` as a booking-domain value object with
      `seatId`, `fullName`, `idDocumentNumber`, `dateOfBirth`, and `gender`
      using the same validation/value-object style as `BookingUserInfo`
- [x] 1.4 Update the `Booking` aggregate to rename `userInfo` to `bookerInfo`,
      add `List<BookingPassenger> passengers`, update `create()` and
      `reconstitute()`, and enforce unique passenger `idDocumentNumber` values ←
      (verify: aggregate creation/reconstitution preserves booker snapshot,
      passenger list, and duplicate-document invariant)

## 2. Backend booking creation flow and validation

- [x] 2.1 Update `CreateBookingRequest` to accept a required `passengers` list
      with nested `PassengerInput` records for `seatId`, `fullName`,
      `idDocumentNumber`, `dateOfBirth`, and `gender`
- [x] 2.2 Update `CreateBookingCommand` to carry passenger payload data from the
      web layer into the use case
- [x] 2.3 Add `BookingError` variants for `TooManySeats`,
      `PassengerSeatMismatch`, `DuplicatePassengerIdDocument`, and
      `InvalidPassengerSeatAssignment`
- [x] 2.4 Update `CreateBookingUseCase` to inject `BookingConfig`, enforce the
      configured seat limit, validate passenger count against selected seats,
      reject duplicate ID documents, reject seat assignments outside `seatIds`,
      and map passenger payloads into `BookingPassenger` objects ← (verify:
      create-booking rejects all four invalid request shapes and creates a valid
      booking with one passenger per selected seat)

## 3. Backend persistence and migration

- [x] 3.1 Create `BookingPassengerSnapshotJson` for passenger JSONB persistence
      serialization
- [x] 3.2 Update `BookingEntity` to add nullable `passengers_snapshot` JSONB
      storage alongside the existing booker snapshot
- [x] 3.3 Update `BookingEntityMapper` to map `BookingPassenger` collections to
      and from `BookingPassengerSnapshotJson`, including null-safe legacy row
      handling
- [x] 3.4 Add a Flyway migration under
      `backend/src/main/resources/db/migration/` to add the nullable
      `passengers_snapshot JSONB` column to `bookings` ← (verify: new bookings
      persist/load passenger snapshots and historical rows without
      `passengers_snapshot` still load successfully)

## 4. Backend read models and API responses

- [x] 4.1 Update booking response DTOs (`BookingResponse`,
      `BookingDetailResponse`) to expose `bookerInfo` and `passengers` with a
      dedicated passenger response shape
- [x] 4.2 Update `PaymentDetailResponse` to expose the passenger list with seat
      assignments for ticket/payment consumers
- [x] 4.3 Update `GetBookingDetailUseCase` to map `bookerInfo` and passengers
      from the aggregate into the new response DTOs
- [x] 4.4 Update `GetPaymentByIdUseCase` to map `bookerInfo`, passenger list,
      and seat-linked passenger data into payment detail output ← (verify:
      booking detail and payment detail APIs return booker vs passenger data in
      a backward-compatible shape for both new and legacy bookings)

## 5. Frontend configuration and booking form foundation

- [x] 5.1 Create `frontend/customer/src/lib/env.ts` with typed access to
      `NEXT_PUBLIC_MAX_SEATS_PER_BOOKING` and a default of `5`
- [x] 5.2 Update `frontend/customer/src/lib/customer-utils.ts` to replace the
      hardcoded `MAX_SEATS_PER_BOOKING = 5` with the typed env wrapper
- [x] 5.3 Add `frontend/customer/.env.example` with
      `NEXT_PUBLIC_MAX_SEATS_PER_BOOKING=5`
- [x] 5.4 Update `seat-selection.tsx` and any dependent selection logic to use
      the frontend-configured max seat limit consistently ← (verify: seat
      selection blocks picks above the configured limit and matches the exported
      env-driven constant everywhere)

## 6. Frontend passenger capture and booking submission

- [x] 6.1 Create a `PassengerForm` component that renders required fields for
      one seat's passenger (`fullName`, `idDocumentNumber`, `dateOfBirth`,
      `gender`) and labels the form with the assigned seat
- [x] 6.2 Create a `PassengerList` component that renders one `PassengerForm`
      per selected seat, aggregates form state, and validates duplicate
      `idDocumentNumber` values across the full list
- [x] 6.3 Refactor `booking-confirmation.tsx` to replace the static
      authenticated passenger card with `PassengerList`, show booker info
      separately, and keep confirm disabled until all passenger forms are
      complete and valid
- [x] 6.4 Update `booking-confirmation.tsx` submission logic to include the
      passengers array in the `createBooking` mutation body ← (verify: review
      page renders one passenger form per seat, duplicate-document errors
      surface before submit, and the booking mutation payload matches the new
      API contract)

## 7. Frontend downstream passenger presentation

- [x] 7.1 Update `frontend/customer/src/components/booking/booking-detail.tsx`
      to render passengers per seat instead of a single passenger summary
- [x] 7.2 Update `frontend/customer/src/components/payment/payment-detail.tsx`
      to render the booking passenger list and seat assignments
- [x] 7.3 Update
      `frontend/customer/src/app/[locale]/(protected)/ticket/[bookingId]/page.tsx`
      to show all passengers with their assigned seats on the consolidated
      ticket view ← (verify: booking detail, payment detail, and ticket pages
      all render multiple passengers correctly and handle legacy no-passenger
      bookings gracefully)

## 8. Localization, contract generation, and verification

- [x] 8.1 Add passenger form labels, passenger section headings, and duplicate/
      required-field validation messages to all locale message files
- [x] 8.2 Update backend OpenAPI-exposed request/response schemas to reflect
      `bookerInfo`, `passengers`, and the new booking validation errors
- [x] 8.3 Regenerate frontend API types from the updated OpenAPI specification
- [x] 8.4 Update root `.env.example` with `BOOKING_MAX_SEATS=5` and add or
      update automated tests covering backend booking validation, persistence
      mapping, and frontend passenger-form flows ← (verify: generated types
      match the new contract, env examples document both seat-limit variables,
      and test coverage exercises the main happy path plus passenger validation
      failures)
