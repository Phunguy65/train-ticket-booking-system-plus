# Why

The current booking flow snapshots the authenticated user as the single
passenger for the entire booking, which prevents one customer from booking
multiple seats for different travelers and loses the seat-to-passenger
relationship needed by payment, booking detail, and ticket views. We need to
separate the booker from the actual passengers now so multi-seat bookings can
capture correct traveler identity data and enforce booking rules consistently
across backend and frontend.

## What Changes

- Update the booking domain and persistence model to keep the authenticated user
  as `bookerInfo` while storing a per-seat passenger list with seat assignment,
  identity document number, birth date, and gender.
- Extend booking creation contracts and validation so each selected seat must be
  paired with exactly one passenger, duplicate passenger ID documents are
  rejected within a booking, and the maximum seats per booking is enforced from
  environment-backed configuration.
- Extend booking, payment, and ticket read models so customer-facing responses
  expose passenger lists instead of a single booking-wide passenger snapshot,
  while remaining backward compatible for existing bookings without passenger
  snapshots.
- Update the customer booking confirmation UI to collect passenger information
  per selected seat, validate duplicate ID documents before submission, and use
  environment-backed seat-limit configuration instead of hardcoded frontend
  constants.
- Update booking detail, payment detail, and printable ticket pages plus locale
  messages to render the per-seat passenger assignments consistently.

## Capabilities

### New Capabilities

- `booking-passenger-assignment`: manage separate booker snapshots and per-seat
  passenger data for booking creation, persistence, validation, and reads.

### Modified Capabilities

- `customer-seat-booking-flow`: booking confirmation requirements now collect,
  validate, and submit one passenger per selected seat instead of reusing the
  authenticated user as the sole passenger.
- `customer-api-contract`: booking and payment schemas now expose `bookerInfo`
  plus passenger lists with seat assignments and validation-driven booking
  errors.
- `customer-ticket-printing-ui`: printable ticket requirements now render all
  passengers with their assigned seats instead of a single passenger summary.
- `i18n`: customer booking, payment, and ticket locales now include passenger
  form labels, duplicate-document validation, and passenger list headings.

## Impact

- Affected backend booking code in the booking aggregate, create-booking use
  case, request/command DTOs, error types, persistence JSON snapshots, entity
  mapping, read use cases, and Flyway migrations.
- Affected backend customer API schemas and generated frontend API types for
  booking, booking detail, and payment detail responses.
- Affected frontend customer booking confirmation, seat-selection utilities,
  booking detail, payment detail, printable ticket page, environment examples,
  and locale message catalogs.
- Adds new configuration surfaces: `BOOKING_MAX_SEATS` for the backend and
  `NEXT_PUBLIC_MAX_SEATS_PER_BOOKING` for the frontend.
