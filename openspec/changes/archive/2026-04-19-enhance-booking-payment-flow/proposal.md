# Why

The customer booking journey currently works functionally, but it gives limited
feedback about progress, payment timing, and booking cost at the moments when
users need that information most. Improving these flow cues now reduces drop-off
between seat selection and payment, especially on mobile, while building on the
existing booking UI and payment redirect behavior already in production.

## What Changes

- Enhance the seat-selection, booking-confirmation, and booking-detail
  experiences with a consistent four-step booking progress indicator.
- Add a mobile-only sticky footer CTA pattern so primary actions and pricing
  stay visible during seat selection and booking review.
- Introduce reusable booking UI components for trip summary, price breakdown,
  and payment-state messaging.
- Improve post-booking payment handling with explicit pending, redirecting,
  success, failed, and expired states plus a visible countdown to payment
  deadline.
- Expand English and Vietnamese localization coverage for the new booking-flow
  labels, summaries, countdown text, and payment states.
- Add component/unit and integration test coverage for the new booking flow UI
  states.

## Capabilities

### New Capabilities

- `customer-payment-status-ui`: reusable payment-status presentation for held,
  redirecting, successful, failed, and expired booking payment states in the
  customer frontend

### Modified Capabilities

- `customer-seat-booking-flow`: booking flow requirements expand to include a
  visible multi-step progress indicator, mobile sticky booking actions,
  collapsible booking summaries, earlier price breakdown visibility, and payment
  countdown/status handling across seat selection, booking confirmation, and
  booking detail pages
- `i18n`: customer booking translations expand to cover stepper labels, payment
  state messages, countdown text, and price-breakdown copy in both supported
  locales

## Impact

- Affected frontend code in `frontend/customer/src/components/ui/`,
  `frontend/customer/src/components/booking/`,
  `frontend/customer/src/components/seats/`, and locale-routed booking pages.
- Affected locale message catalogs in `frontend/customer/src/messages/en.json`
  and `frontend/customer/src/messages/vi.json`.
- Adds new reusable UI surface area that should follow existing shadcn/ui,
  Radix, Tailwind v4, and `next-intl` patterns in the customer app.
- Adds Vitest + React Testing Library coverage for new components and booking
  flow states.
- Continues to depend on existing booking creation/detail APIs and external
  payment checkout URL redirects without changing backend contracts.
