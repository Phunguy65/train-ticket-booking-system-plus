# Tasks

## 1. Backend payment history and enriched payment detail

- [x] 1.1 Add payment application query/response types for user payment history
      (`GetUserPaymentsQuery`, `UserPaymentResponse`) following existing
      paginated query patterns
- [x] 1.2 Extend the payment repository contract and persistence adapter with a
      `findByUserId()` query that returns payment summary data with booking
      route/date information
- [x] 1.3 Implement `GetUserPaymentsUseCase` with owner-only authorization and
      paginated response mapping
- [x] 1.4 Expose `GET /api/v1/users/{userId}/payments` through the backend web
      layer with request validation, authentication-derived requesting user ID,
      and OpenAPI metadata
- [x] 1.5 Enrich payment detail response assembly to include booking status,
      passenger info, seats, and trip data needed for ticket rendering ←
      (verify: history endpoint is owner-scoped, page schema matches the spec,
      and payment detail returns all ticket-ready booking fields)

## 2. Contract and backend verification coverage

- [x] 2.1 Update backend response annotations and generated contract inputs so
      the public customer API includes the new user payments operation and
      enriched payment detail schema
- [x] 2.2 Regenerate or refresh shared OpenAPI/customer SDK artifacts consumed
      by the frontend
- [x] 2.3 Add backend unit tests for `GetUserPaymentsUseCase`, including success
      and forbidden-access scenarios, plus tests covering enriched payment
      detail mapping ← (verify: generated contract exposes the new
      endpoint/schema and backend tests prove both authorization and response
      enrichment)

## 3. Frontend account payments and payment detail UI

- [x] 3.1 Refactor the protected account page into tab-based navigation for
      bookings and payments while preserving current bookings behavior
- [x] 3.2 Create account payment history components (`PaymentsList`,
      `PaymentCard`) with loading, error, and empty states aligned to existing
      patterns
- [x] 3.3 Add frontend query helpers/hooks usage for the user payments endpoint
      and wire retry/error handling through existing toast and query conventions
- [x] 3.4 Create `PaymentStatusBadge` with icon + text accessibility behavior
      and add a `success` variant to the shared Badge component
- [x] 3.5 Add the localized `/payment/[id]` route and `PaymentDetail` component
      showing payment metadata, linked booking summary, and conditional
      print-ticket action for paid payments ← (verify: account tabs, payment
      cards, status badges, and payment detail route behave consistently across
      loading/error/paid/unpaid states)

## 4. Ticket printing experience

- [x] 4.1 Add the `/ticket/[bookingId]` route and `TicketPrint` page/component
      that renders passenger, train, route, timing, and seat information from
      API data
- [x] 4.2 Add `TicketQRCode` using `qrcode.react` and define the QR payload
      sourced from booking-related ticket data
- [x] 4.3 Implement browser print and Web Share API actions with capability
      checks and graceful fallback behavior
- [x] 4.4 Add print-specific styling for clean output and hide non-ticket
      controls during printing ← (verify: the ticket page opens from paid
      payment detail, renders QR/ticket data correctly, and prints cleanly with
      non-print UI removed)

## 5. Localization and automated frontend tests

- [x] 5.1 Add English translation keys for payment history, payment statuses,
      payment detail labels, and ticket print/share text
- [x] 5.2 Add Vietnamese translation keys for payment history, payment statuses,
      payment detail labels, and ticket print/share text
- [x] 5.3 Add frontend component/page tests covering account payments tabs,
      payment status badge variants, payment detail conditional print action,
      and ticket print/share interactions ← (verify: both locales render the new
      payment/ticket copy and frontend tests cover key history/detail/print user
      flows)
