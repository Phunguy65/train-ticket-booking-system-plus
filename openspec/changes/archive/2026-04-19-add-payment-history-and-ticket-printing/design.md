# Context

The current train ticket booking system already supports booking creation,
checkout handoff, booking history, and booking detail pages, but the
post-payment experience remains fragmented. On the backend, customer-facing
booking endpoints already enforce self-access checks and assemble nested booking
detail data through application use cases and repository projections. On the
frontend, the protected account page currently renders a bookings list only,
while payment presentation is centered around booking flow status rather than
dedicated payment management.

This change spans backend booking/payment modules, generated customer API
contracts, frontend protected account navigation, new localized payment/ticket
routes, and test coverage. The implementation must stay aligned with the
existing hexagonal backend structure, JSend/OpenAPI conventions, Next.js App
Router, the generated TanStack Query SDK, shadcn/ui component patterns, and
`next-intl` localization.

## Goals / Non-Goals

**Goals:**

- Add a backend payment-history query that returns only the authenticated
  customer's payments in a paginated shape suitable for the account dashboard.
- Enrich payment detail responses with the booking, passenger, seat, and trip
  data required for ticket printing without introducing a separate ticket
  backend.
- Extend the protected account UI with tab-based navigation for bookings and
  payments while preserving existing loading, error, and empty-state patterns.
- Add localized payment detail and ticket print pages that reuse generated API
  data, accessible status presentation, and client-side print/share behaviors.
- Keep the feature testable through backend use-case tests and frontend
  component/page tests.

**Non-Goals:**

- Generating server-side HTML or PDF tickets.
- Introducing new payment gateways, refund flows, or admin payment tooling.
- Redesigning the existing booking creation and checkout flow.
- Adding background jobs, email delivery, or offline ticket storage.
- Changing the authenticated access model beyond enforcing owner-only payment
  visibility.

## Decisions

### 1. Model payment history as a payment-module query exposed through a user-scoped endpoint

The backend will add `GetUserPaymentsUseCase`, `GetUserPaymentsQuery`, and a
list-item response model in the payment module, while exposing the API as
`GET /api/v1/users/{userId}/payments` to match the existing user-scoped bookings
endpoint.

- **Why this approach:** it preserves the current API style, keeps payment list
  rules inside the payment module, and reuses the established authorization
  check pattern from `GetUserBookingsUseCase` (`userId` must equal
  `requestingUserId`).
- **Alternative considered:** add a generic `GET /api/v1/payments` endpoint that
  derives the user from authentication only. Rejected because it diverges from
  the current public API style and makes the account section less symmetric with
  the bookings API.

### 2. Extend repository projections instead of composing payment detail from multiple endpoint calls

Payment detail data needed for printing will be assembled on the backend through
an enriched response shape backed by repository queries/projections that already
join payment, booking, seat, and scheduled-trip data.

- **Why this approach:** the ticket page needs a single authoritative payload,
  avoids multiple frontend round-trips, and keeps printing resilient to partial
  client fetch failures.
- **Alternative considered:** fetch payment data plus booking detail separately
  on the frontend and merge them in React. Rejected because it duplicates
  mapping logic, creates consistency risks, and complicates authorization/error
  handling.

### 3. Keep ticket generation frontend-only and route-based

The frontend will render a dedicated `/[locale]/ticket/[bookingId]` page using
JSON data returned from payment/booking APIs, and the payment detail page will
open that route in a new tab for printing.

- **Why this approach:** it follows the explicit product decision to avoid
  backend HTML/PDF generation, fits the existing Next.js routing model, and lets
  browser print behavior remain native.
- **Alternative considered:** return pre-rendered ticket markup or PDFs from the
  backend. Rejected because it adds server rendering complexity and makes UI
  iteration slower.

### 4. Extend the account area with tabs instead of adding a separate payments page

The protected account page will become a tabbed container with localized
bookings and payments tabs. Each tab will own its list rendering, query
lifecycle, and empty/error states while preserving the single account entry
point.

- **Why this approach:** it matches the stated UX direction, keeps account
  navigation compact, and reuses the current protected route/layout.
- **Alternative considered:** add a dedicated `/account/payments` route.
  Rejected because it splits related account history into multiple entry points
  and conflicts with the tab-based design decision.

### 5. Treat payment detail status display as an extension of the shared payment status capability

The frontend will add a reusable `PaymentStatusBadge` component and extend the
shared badge styles with a `success` variant so payment cards and payment detail
views can show consistent, accessible status cues with icon + text.

- **Why this approach:** the booking flow already has a payment status
  capability, and extending it keeps status semantics, translation keys, and
  visual patterns aligned.
- **Alternative considered:** render ad-hoc colored text/badges directly in each
  payment component. Rejected because it would fragment accessibility and status
  mapping logic.

### 6. Keep OpenAPI/SDK generation in scope because frontend data contracts change

The new user payments endpoint and enriched payment detail schema will be
exposed through backend annotations so the generated customer OpenAPI contract
and frontend query helpers can be regenerated instead of hand-written.

- **Why this approach:** the project already standardizes on generated customer
  SDKs, and the new frontend pages depend on stable generated types and options
  helpers.
- **Alternative considered:** hand-write temporary frontend fetchers for the new
  endpoints. Rejected because it bypasses the project contract workflow and
  would create duplicate API types.

## Risks / Trade-offs

- **[Risk] Enriched payment detail payloads may duplicate data already available
  in booking detail responses** → **Mitigation:** keep the nested structure
  focused on payment and ticket rendering needs, and reuse existing response
  sub-shapes where practical.
- **[Risk] User-scoped payment queries may expose another customer's data if
  authorization checks are missed in any code path** → **Mitigation:** enforce
  the same owner check in the use case before repository access and cover
  forbidden scenarios with unit tests.
- **[Risk] Printable ticket layouts can degrade on different browsers and mobile
  devices** → **Mitigation:** keep the print page simple, use dedicated
  `@media print` styling, and isolate interactive controls outside the printable
  content region.
- **[Risk] Web Share API support is inconsistent across browsers** →
  **Mitigation:** gate the share button behind capability detection and keep
  print as the primary action.
- **[Risk] A separate ticket route opened in a new tab can fail if popup
  blockers or navigation timing interfere** → **Mitigation:** navigate through a
  normal anchor/button-to-link interaction rather than window-open side effects.

## Migration Plan

1. Add backend payment history and enriched payment detail support, including
   repository changes, controller wiring, response models, OpenAPI annotations,
   and unit tests.
2. Regenerate or refresh the shared customer API contract and frontend generated
   SDK artifacts for the new/changed payment endpoints.
3. Add frontend account tab composition, payment list/detail components, and the
   accessible payment status badge.
4. Add the printable ticket route, ticket components, QR rendering dependency,
   print styles, and share behavior.
5. Extend English/Vietnamese locale catalogs and add frontend tests for account
   payments, payment detail, and ticket print UI.
6. Roll back, if needed, by removing the new routes/components and reverting the
   contract changes; the existing booking and payment callback flows remain
   functional without payment history or printing.

## Open Questions

- The design assumes an existing customer-facing payment detail endpoint can be
  safely enriched; implementation should confirm whether that endpoint lives in
  the booking controller, payment controller, or both, and update the canonical
  operation accordingly.
- The ticket page needs a QR payload format; implementation should confirm
  whether the QR should encode booking ID only, a localized deep link, or a
  small ticket summary string.
- The payment history list includes a booking summary with route and date;
  implementation should confirm the exact trip date field to expose when
  journeys span time zones or overnight arrivals.
