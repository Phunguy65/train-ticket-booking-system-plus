# Why

The customer frontend currently stops at authentication, which prevents users
from completing the core train-ticket booking journey in the web app even though
the backend APIs and generated hooks are already available. Building the
customer-facing booking UI now unlocks end-to-end trip discovery, seat
selection, booking management, and profile management on top of the existing
Next.js platform.

## What Changes

- Add a customer homepage experience with a localized trip search form, station
  autocomplete, departure date selection, and query-param navigation to search
  results.
- Add a searchable trip results flow with sorting, filtering, cursor-based
  infinite scrolling, skeleton loading states, and an empty state.
- Add a seat-selection experience with coach tabs, visual seat maps, seat-state
  indicators, multi-seat selection limits, and booking handoff.
- Add a booking confirmation flow that uses authenticated user information,
  creates a booking, displays the payment deadline, and redirects to checkout.
- Add protected account and profile pages for booking history, booking detail
  access, cancellation, and profile editing.
- Add shared customer navigation, responsive layout structure, authenticated
  user menus, and full vi/en translation coverage for all new UI.
- Add supporting validation, error handling, accessibility behaviors, and
  automated test coverage for critical booking flows.

## Capabilities

### New Capabilities

- `customer-trip-search-ui`: Covers the homepage search form and the `/search`
  results experience, including station autocomplete, filters, sorting, infinite
  pagination, loading states, and empty states.
- `customer-seat-booking-flow`: Covers seat selection, booking confirmation,
  booking creation, payment redirect, and booking detail presentation for a
  selected trip.
- `customer-account-ui`: Covers protected customer account pages, bookings
  list/detail access, booking cancellation, and profile viewing/editing.
- `customer-navigation-ui`: Covers the shared main layout, header, locale
  switcher, mobile navigation, auth-aware user actions, and responsive shell for
  customer pages.

### Modified Capabilities

- `i18n`: Extend translated UI requirements beyond auth pages so all new
  customer booking pages, labels, statuses, actions, and feedback messages are
  available in both Vietnamese and English.

## Impact

- Affected frontend routes: `frontend/customer/src/app/[locale]/page.tsx`, new
  `(main)` route group pages, new `(protected)` route group pages, and related
  layouts.
- Affected frontend components: shared navigation, search form components, trip
  cards/lists, seat map widgets, booking summary components, account/profile
  views, and additional shadcn/ui primitives.
- Affected frontend state/data access: TanStack Query hooks generated from the
  existing OpenAPI contract, route query-param parsing, protected-route auth
  checks, and booking/profile mutations.
- Affected localization and validation: `src/messages/{vi,en}.json`,
  react-hook-form + zod schemas, translated status labels, error messages, and
  accessibility copy.
- Affected testing: unit, component, and integration coverage for search and
  booking flows in the customer app.
