# Why

The system currently has a two-phase booking flow (HELD → CONFIRMED) but no payment processing. Users can reserve seats but there is no mechanism to collect payment or enforce a reservation window. Integrating Stripe Checkout Session provides a hosted, PCI-compliant payment UI and uses the session lifecycle as the single source of truth for reservation expiry — eliminating the need for a separate backend timer.

## What Changes

- Add `stripe-java` SDK dependency to the backend
- Introduce a new `payment` vertical slice module (Spring Modulith) responsible for Stripe Checkout Session creation, webhook handling, and payment lifecycle management
- Extend the `booking` module: `CreateSeatHoldUseCase` now creates a Stripe Checkout Session atomically with the hold; cancellation expires the session
- Replace the current `ExpireHoldsJob` polling-by-deadline approach with a Stripe-webhook-driven expiry flow, backed by a lightweight reconciliation job
- Add a `StripeWebhookController` to handle `checkout.session.completed` and `checkout.session.expired` events
- Add Flyway migration for a `payments` table (replacing the unused `transactions` table stub)
- Add Stripe configuration properties (`STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`, `STRIPE_SUCCESS_URL`, `STRIPE_CANCEL_URL`)

## Capabilities

### New Capabilities

- `stripe-checkout-payment`: Stripe Checkout Session integration — session creation tied to seat hold, webhook-driven confirmation and expiry, cancellation flow, and reconciliation fallback

### Modified Capabilities

- `backend-booking-slice`: Booking lifecycle states change from `PENDING → CONFIRMED → CANCELLED` to `HELD → CONFIRMED → CANCELLED`; `CreateSeatHoldUseCase` now creates a Checkout Session atomically; cancellation triggers `checkout.sessions.expire()`; `ExpireHoldsJob` is replaced by webhook + reconciliation
- `database-schema`: New Flyway migration adds `payments` table and `checkout_session_id` column to `bookings`

## Impact

- **Backend**: New `payment` Spring Modulith module; changes to `booking` module use cases and domain model
- **API**: New endpoints — `POST /api/v1/webhooks/stripe` (raw body, Stripe signature verification); `DELETE /api/v1/bookings/{id}` updated to expire Checkout Session
- **Database**: New migration `V{n}__add_payments_table.sql`
- **Dependencies**: Add `com.stripe:stripe-java` to `build.gradle.kts`
- **Configuration**: New environment variables for Stripe keys and redirect URLs
- **Frontend**: Receives `checkoutUrl` from hold response and redirects user to Stripe-hosted page
