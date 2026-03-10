# Context

The train ticket booking system has a complete booking lifecycle (HELD → CONFIRMED → CANCELLED) with 15-minute seat holds, but no payment processing. The `payments` table, Stripe SDK dependency, and `application.yaml` Stripe config already exist. The `BookingCancelled` domain event already carries a `requiresRefund` flag. The missing piece is the `payment` Spring Modulith module that wires Stripe into the booking lifecycle.

## Goals / Non-Goals

**Goals:**

- Create Stripe Checkout Sessions when a booking is created (HELD state)
- Confirm bookings and transition seats to BOOKED on successful payment via webhook
- Issue full refunds automatically when a confirmed booking is cancelled
- Expire Stripe sessions when the 15-minute hold expires
- Handle the edge case where payment arrives after hold expiry (refund immediately)
- Idempotent webhook processing using `stripe_event_id`

**Non-Goals:**

- Partial refunds (only full booking cancellation is in scope)
- Subscription or recurring payments
- Multi-currency support (VND only for now)
- Frontend payment UI (backend only)
- Admin manual refund tooling

## Decisions

### Decision 1: Stripe Checkout Sessions over Payment Intents API

**Choice**: Use `stripe.checkout.sessions.create()` (hosted Checkout page) rather than building a custom Payment Intents flow.

**Rationale**: The `payments` table already has `checkout_session_id` and `stripe_payment_intent_id` columns, indicating this was the intended approach. Checkout Sessions handle PCI compliance, payment method rendering, and localization out of the box. The frontend only needs to redirect to the Stripe-hosted URL.

**Alternative considered**: Custom Payment Intents with Stripe Elements — rejected because it requires frontend work and more complex lifecycle management.

### Decision 2: Event-Driven Integration via Spring Modulith Domain Events

**Choice**: The `payment` module listens to `BookingCreated`, `BookingCancelled`, and booking expiry events rather than being called directly from booking use cases.

**Rationale**: Keeps the `booking` module unaware of payment concerns. Spring Modulith's `@ApplicationModuleListener` provides transactional event publication — the event is only delivered after the booking transaction commits, preventing race conditions.

**Alternative considered**: Direct service call from `CreateBookingUseCase` — rejected because it creates tight coupling between modules and violates Spring Modulith boundaries.

### Decision 3: Stripe Session Expiry = 30 min, Backend Enforces 15 min

**Choice**: Create Stripe sessions with `expires_at = now + 30 minutes` (Stripe's minimum), but the existing `BookingExpiryScheduler` (runs every 60s) expires bookings at 15 minutes and calls `stripe.checkout.sessions.expire(sessionId)` to cancel the Stripe session.

**Rationale**: Stripe does not support sessions shorter than 30 minutes. The backend is the source of truth for the 15-minute window. Expiring the session via API prevents customers from paying after the hold is released.

### Decision 4: Webhook Idempotency via `stripe_event_id` Unique Constraint

**Choice**: Before processing any webhook event, check if `stripe_event_id` already exists in the `payments` table. The DB unique constraint on `stripe_event_id` acts as the idempotency guard.

**Rationale**: Stripe delivers webhooks with at-least-once semantics. The existing `payments` table already has `stripe_event_id VARCHAR(255) UNIQUE`, making this a zero-cost idempotency mechanism.

### Decision 5: Refund Triggered by `BookingCancelled` Domain Event

**Choice**: `OnBookingCancelledListener` in the `payment` module listens for `BookingCancelled(requiresRefund=true)` and calls `RefundPaymentUseCase`.

**Rationale**: The `booking` module already publishes this event with the correct flag. No changes to cancellation logic are needed. The payment module handles its own concern (refunding) in response to a booking-level fact.

### Decision 6: Add `confirmHeldSeats()` to `RouteSeatAvailabilityPort`

**Choice**: Add a new method to the existing port interface in the `train` module to transition seats from `HELD → BOOKED` when payment succeeds.

**Rationale**: The current port only has `holdSeats`, `releaseHeldSeats`, and `cancelBookedSeats`. Payment confirmation requires a new transition. This is a minimal, additive change to an existing port.

## Risks / Trade-offs

**[Risk] Payment arrives after 15-minute hold expires** → The `HandlePaymentSuccessUseCase` checks booking status after receiving `checkout.session.completed`. If booking is `CANCELLED`, it immediately calls `stripe.refunds.create()` and marks payment as `REFUNDED`. The customer receives a refund automatically.

**[Risk] Webhook delivery delay causes booking to expire before confirmation** → Mitigated by the grace-period check above. The scheduler runs every 60s, so there is a window where a booking could expire while payment is in-flight. The refund path handles this cleanly.

**[Risk] `OptimisticLockException` during seat confirmation** → `HandlePaymentSuccessUseCase` must handle `OptimisticLockException` from `confirmHeldSeats()`. If thrown, the webhook handler returns HTTP 500, causing Stripe to retry. On retry, idempotency check prevents double-processing. Seats will be confirmed on the retry.

**[Risk] Stripe session expires at 30 min but booking expired at 15 min** → The `OnBookingExpiredListener` calls `stripe.checkout.sessions.expire()` immediately when a booking expires, closing the 15-minute gap. If the API call fails, the session will naturally expire at 30 minutes, but the seat is already released at 15 minutes — the payment-after-expiry refund path handles any late payments.

**[Trade-off] No retry queue for failed refunds** → If `stripe.refunds.create()` fails (network error, Stripe outage), the refund is lost. Mitigation: log the failure with full context and alert. A retry queue (BullMQ/Redis) is out of scope for this change but should be added in a follow-up.

## Module Structure

```
payment/
├── package-info.java                          # @ApplicationModule
├── domain/
│   ├── model/
│   │   ├── Payment.java                       # Aggregate root
│   │   ├── PaymentId.java                     # UUID wrapper
│   │   └── PaymentStatus.java                 # PENDING | PAID | CANCELLED | FAILED | REFUNDED
│   ├── event/
│   │   ├── PaymentCompleted.java
│   │   └── PaymentRefunded.java
│   ├── repository/
│   │   └── PaymentRepository.java
│   └── error/
│       └── PaymentError.java
├── application/
│   ├── usecase/
│   │   ├── CreateCheckoutSessionUseCase.java   # BookingCreated → Stripe session
│   │   ├── HandlePaymentSuccessUseCase.java    # webhook: session.completed
│   │   ├── RefundPaymentUseCase.java           # BookingCancelled(requiresRefund=true)
│   │   └── ExpireCheckoutSessionUseCase.java   # booking expired → expire Stripe session
│   ├── command/
│   │   └── CreateCheckoutSessionCommand.java
│   ├── dto/
│   │   └── PaymentDto.java
│   ├── port/
│   │   └── StripeGatewayPort.java             # Abstracts Stripe SDK calls
│   └── listener/
│       ├── OnBookingCreatedListener.java       # → CreateCheckoutSessionUseCase
│       ├── OnBookingCancelledListener.java     # → RefundPaymentUseCase
│       └── OnBookingExpiredListener.java       # → ExpireCheckoutSessionUseCase
└── infrastructure/
    ├── persistence/
    │   ├── PaymentEntity.java
    │   ├── PaymentJpaRepository.java
    │   ├── PaymentRepositoryAdapter.java
    │   └── PaymentEntityMapper.java
    ├── stripe/
    │   └── StripeGatewayAdapter.java          # Implements StripeGatewayPort
    └── web/
        ├── StripeWebhookController.java        # POST /api/v1/webhooks/stripe
        ├── PaymentController.java              # GET /api/v1/payments/{bookingId}
        └── PaymentHttpResponse.java
```

## Payment Lifecycle State Machine

```
Booking HELD
     │
     │ BookingCreated event
     ▼
Payment PENDING ──────────────────────────────────────────┐
     │                                                    │
     │ checkout.session.completed                         │ checkout.session.expired
     ▼                                                    │ (booking expired at 15min)
Payment PAID                                              ▼
     │                                              Payment CANCELLED
     │ BookingCancelled(requiresRefund=true)
     ▼
Payment REFUNDED

     │ payment_intent.payment_failed
     ▼
Payment FAILED
```

## Cross-Module Dependencies

```
payment module
  ├── listens to: booking::events (BookingCreated, BookingCancelled)
  ├── calls: train::RouteSeatAvailabilityPort (confirmHeldSeats)
  └── reads: booking::BookingRepository (to check status on webhook)
```

Spring Modulith `allowedDependencies` in `payment/package-info.java`:
- `booking::events` — to receive domain events
- `train::ports` — to call `confirmHeldSeats()`

## Migration Plan

1. Deploy with `STRIPE_SECRET_KEY` and `STRIPE_WEBHOOK_SECRET` env vars set (test keys first)
2. Register webhook endpoint in Stripe Dashboard: `POST /api/v1/webhooks/stripe`
3. Subscribe to events: `checkout.session.completed`, `checkout.session.expired`, `charge.refunded`, `payment_intent.payment_failed`
4. Test with Stripe CLI: `stripe listen --forward-to localhost:8080/api/v1/webhooks/stripe`
5. Verify `payments` table is populated correctly on booking creation
6. Switch to live keys after QA sign-off

**Rollback**: Feature flag or module disable. The `payment` module is additive — removing it reverts to the pre-payment state without affecting booking or seat logic.

## Open Questions

- Should `GET /api/v1/payments/{bookingId}` be accessible to the booking owner only, or also to admins? (Assume owner + admin for now)
- What is the VND amount unit for Stripe? (Stripe treats VND as a zero-decimal currency — amounts are in whole dong, not cents)
- Should failed payments trigger a notification to the customer? (Out of scope for this change, but the `PaymentFailed` domain event can be consumed by a future notification module)
