# Why

The system currently supports booking and seat reservation but has no payment processing. Customers can hold seats for 15 minutes but there is no mechanism to collect payment, confirm bookings, or issue refunds on cancellation. Stripe integration is needed to complete the booking lifecycle.

## What Changes

- Add a `payment` Spring Modulith module with full Vertical Slice Architecture
- Implement Stripe Checkout Session creation triggered by `BookingCreated` domain event
- Implement webhook handler for `checkout.session.completed` to confirm bookings and transition seats from `HELD → BOOKED`
- Implement automatic full refund via Stripe Refunds API when `BookingCancelled(requiresRefund=true)` event is received
- Expire Stripe Checkout Sessions when the 15-minute hold expires (via existing `BookingExpiryScheduler`)
- Add `confirmHeldSeats()` to `RouteSeatAvailabilityPort` to support `HELD → BOOKED` seat transition on payment success
- Expose `GET /api/v1/payments/{bookingId}` endpoint for payment status polling

## Capabilities

### New Capabilities

- `payment-checkout`: Create Stripe Checkout Sessions on booking creation, return checkout URL to client, track payment lifecycle in `payments` table
- `payment-webhook`: Handle Stripe webhook events (session completed, session expired, charge refunded, payment failed) with signature verification and idempotent processing
- `payment-refund`: Automatically issue full Stripe refund when a confirmed booking is cancelled; handle edge case where payment arrives after hold expiry

### Modified Capabilities

- `backend-booking-slice`: Add `confirmHeldSeats()` to `RouteSeatAvailabilityPort` to support `HELD → BOOKED` transition; `BookingCancelled` event already carries `requiresRefund` flag (no spec change needed beyond this port addition)

## Impact

- **New module**: `backend/src/main/java/.../payment/` (domain, application, infrastructure layers)
- **New DB migration**: `payments` table already exists in schema; no new migration needed
- **Modified**: `RouteSeatAvailabilityPort` — add `confirmHeldSeats(bookingId)` method
- **New env vars**: `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`, `STRIPE_SUCCESS_URL`, `STRIPE_CANCEL_URL` (already in `application.yaml`)
- **Dependencies**: `stripe-java` SDK already in `build.gradle.kts`
- **APIs**: New `POST /api/v1/webhooks/stripe` (public, no auth), `GET /api/v1/payments/{bookingId}` (authenticated)
