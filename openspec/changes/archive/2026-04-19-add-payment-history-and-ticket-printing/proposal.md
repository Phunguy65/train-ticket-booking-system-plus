# Why

Customers can currently complete payments but cannot review a payment history,
inspect a payment with ticket-ready booking details, or print/share a ticket
from the web app. Adding these account and post-payment capabilities now closes
a key gap in the end-to-end booking journey and makes paid bookings more useful
after checkout succeeds.

## What Changes

- Add a paginated customer payment history API and frontend account tab so users
  can browse past payments without leaving the account area.
- Enrich payment detail data with booking, passenger, seat, and trip information
  needed for ticket presentation and printing.
- Add localized payment detail UI with accessible payment-status badges and a
  print-ticket action that is only available for paid payments.
- Add a dedicated printable ticket page that renders ticket content from JSON,
  supports browser printing, mobile sharing, and client-side QR code generation.
- Extend English and Vietnamese translations for payment history, payment
  statuses, payment detail content, and ticket print labels.
- Add backend use-case/repository tests and frontend component/page tests for
  the new payment and ticket flows.

## Capabilities

### New Capabilities

- `customer-api-contract`: Covers the public customer API contract for user
  payment history and enriched payment detail response shapes exposed by backend
  endpoints and consumed by generated frontend SDKs.
- `customer-ticket-printing-ui`: Covers the localized
  `/[locale]/ticket/[bookingId]` printable ticket experience, including print
  layout, browser print action, mobile share action, and client-side QR code
  rendering.

### Modified Capabilities

- `customer-account-ui`: Extend the protected account area to include tab-based
  payment history, payment cards, payment detail access, and
  payment-detail-driven ticket printing entry points.
- `customer-payment-status-ui`: Extend payment status presentation to support
  accessible payment badges and payment-detail contexts in addition to booking
  flow status messaging.
- `i18n`: Extend localized customer copy to cover payment history, payment
  statuses, payment detail labels, and ticket print text in both supported
  locales.

## Impact

- Affected backend code in booking and payment application/domain/infrastructure
  layers, including controller mappings, use cases, queries, repository ports,
  JPA adapters, and response models.
- Affected frontend routes and components in
  `frontend/customer/src/app/[locale]/(protected)/account/`,
  `frontend/customer/src/app/[locale]/payment/[id]`,
  `frontend/customer/src/app/[locale]/ticket/[bookingId]`, and component folders
  under `account`, `payment`, and `ticket`.
- Affected generated SDK/OpenAPI artifacts because the public customer API
  surface changes.
- Adds dependency usage for client-side QR rendering via `qrcode.react` in the
  customer frontend.
- Affected locale catalogs and automated tests for backend use cases and
  frontend UI states.
