# Context

The backend has a working two-phase booking flow: `CreateSeatHoldUseCase` locks seats and creates a `Booking` in `HELD` status with a `paymentDeadline`; `ConfirmSeatHoldUseCase` transitions it to `CONFIRMED`; `ExpireHoldsJob` polls every 60 seconds to cancel expired holds. A `transactions` table stub exists but has no domain model or payment logic. There is no Stripe integration.

The goal is to wire Stripe Checkout Session into this existing flow so that the session lifecycle drives reservation expiry — no separate backend timer needed.

## Goals / Non-Goals

**Goals:**

- Create a `payment` Spring Modulith module owning all Stripe interaction
- Atomically create a Checkout Session when a seat hold is created
- Use `checkout.session.completed` and `checkout.session.expired` webhooks as the authoritative signal for booking confirmation and cancellation
- Expire the Checkout Session when the user or system cancels a booking
- Provide a reconciliation job as a fallback for missed webhooks
- Support VND as a zero-decimal currency

**Non-Goals:**

- Frontend Stripe.js integration (frontend only receives `checkoutUrl` and redirects)
- Support for async payment methods (bank transfer, SEPA) — card only for now
- Refunds or partial captures
- Multi-currency support beyond VND

## Decisions

### Decision 1: Checkout Session as reservation source of truth

**Choice**: Stripe Checkout Session lifecycle drives booking state — no separate `paymentDeadline` enforcement.

**Rationale**: Stripe's minimum session expiry is 30 minutes. Rather than fighting this with a backend timer, we accept 30 minutes as the reservation window and let `checkout.session.expired` trigger seat release. This eliminates the `ExpireHoldsJob` deadline-polling logic and reduces moving parts.

**Alternative considered**: Keep `ExpireHoldsJob` at 15 minutes and call `sessions.expire()` at the deadline. Rejected because it adds complexity (two expiry mechanisms) and the 15-minute requirement was relaxed.

**`after_expiration.recovery` disabled**: Re-opening a session after expiry could allow payment on already-released seats. Disabled unconditionally.

---

### Decision 2: `payment` module owns Stripe, `booking` module owns lifecycle

**Choice**: A new `payment` Spring Modulith module owns `CheckoutSession` aggregate, `StripeCheckoutPort`, `StripeWebhookController`, and all Stripe SDK calls. The `booking` module calls into `payment` via a published interface (`@NamedInterface("api")`).

**Rationale**: Keeps Stripe SDK confined to one module. The `booking` module stays framework-agnostic at the domain level. Cross-module communication uses Spring Modulith's `@ApplicationModuleListener` for async events and direct port injection for synchronous calls (session creation at hold time).

**Module dependency**: `booking` → `payment::api` (one-way). `payment` listens to `BookingCancelled` domain event to expire sessions.

```
booking module                    payment module
──────────────────────────────    ──────────────────────────────
CreateSeatHoldUseCase             CheckoutSessionPort (interface)
  └─ calls CheckoutSessionPort ──►  StripeCheckoutAdapter (impl)
                                      └─ stripe.checkout.sessions.create()

CancelBookingUseCase              @ApplicationModuleListener
  └─ publishes BookingCancelled ──► ExpireCheckoutSessionOnCancelListener
                                      └─ stripe.checkout.sessions.expire()

                                  StripeWebhookController
                                    ├─ checkout.session.completed
                                    │    └─ ConfirmBookingOnPaymentUseCase
                                    └─ checkout.session.expired
                                         └─ CancelBookingOnExpiryUseCase
```

---

### Decision 3: Atomic hold + session creation in one transaction boundary

**Choice**: `CreateSeatHoldUseCase` calls `CheckoutSessionPort.create()` inside the same `@Transactional` method, after the booking is persisted.

**Rationale**: If Stripe call fails, the transaction rolls back and no orphaned hold exists. Idempotency key (`booking.idempotencyKey`) is passed to Stripe so retries are safe.

**Risk**: Stripe API latency (~200-500ms) inside a DB transaction holds the pessimistic lock longer. Acceptable given low concurrency on individual seats; lock timeout is already 3 seconds.

**Alternative considered**: Create hold first, then call Stripe outside the transaction. Rejected — leaves a window where hold exists but no session, requiring compensating logic.

---

### Decision 4: `cancel_url` triggers server-side session expiry

**Choice**: `cancel_url` points to a backend endpoint (`GET /api/v1/bookings/{id}/cancel-redirect`) that calls `sessions.expire()` and redirects the user to the frontend cancel page.

**Rationale**: Stripe does not expire a session when the user navigates to `cancel_url`. Without explicit expiry, the session stays `open` for 30 minutes, holding the seat. Routing through the backend ensures immediate release.

**Alternative considered**: Frontend calls `DELETE /api/v1/bookings/{id}` on cancel_url landing. Rejected — requires frontend coordination and is less reliable (user may close browser).

---

### Decision 5: Reconciliation job replaces `ExpireHoldsJob`

**Choice**: A `PaymentReconciliationJob` runs every 5 minutes. It queries bookings in `HELD` status older than 35 minutes (30-min session + 5-min buffer), retrieves each session from Stripe, and applies the correct state transition if the webhook was missed.

**Rationale**: Webhooks are reliable but not guaranteed (server downtime, network issues). The reconciliation job is a safety net, not the primary mechanism. 35-minute threshold avoids false positives during normal payment flow.

---

### Decision 6: `payments` table replaces `transactions` stub

**Choice**: Drop the existing `transactions` table stub and create a `payments` table with Stripe-specific fields. Add `checkout_session_id` column to `bookings`.

**Rationale**: The `transactions` table has no domain model, no entity, and a hardcoded `gateway = 'SEPAY'` default that is irrelevant. A clean `payments` table avoids confusion.

**Migration**: New Flyway migration `V{n}__add_stripe_payments.sql`.

---

### Decision 7: VND zero-decimal handling

**Choice**: A `StripeAmountConverter` utility converts `BigDecimal` VND amounts to `Long` by calling `.longValue()` directly (no multiplication by 100). Currency is hardcoded to `"vnd"` for now.

**Rationale**: VND is a zero-decimal currency in Stripe. Multiplying by 100 would charge 100× the intended amount.

## Risks / Trade-offs

**[Webhook delay]** → `checkout.session.expired` fires 10-60 seconds after the session's `expires_at`. Seats are held slightly longer than 30 minutes. Mitigation: reconciliation job at 35-minute threshold catches any stragglers.

**[Race condition: payment at expiry boundary]** → User submits payment at minute 29:59; `checkout.session.expired` webhook arrives before `checkout.session.completed`. Stripe guarantees a session cannot be both `complete` and `expired` — once `complete`, it will never transition to `expired`. The reconciliation job checks `session.status == "complete"` before cancelling.

**[Stripe API latency in transaction]** → Pessimistic lock held during Stripe HTTP call (~200-500ms). Mitigation: lock timeout is 3 seconds; Stripe p99 latency is well under this. Monitor with metrics.

**[Missed webhook + server downtime]** → If server is down for >35 minutes, reconciliation job catches up on restart. Stripe retries webhooks for 3 days in live mode.

**[cancel_url reliability]** → If user closes browser without hitting cancel_url, session stays open until natural expiry (30 min). This is acceptable — the seat is released at session expiry via webhook.

## Migration Plan

1. Add `stripe-java` dependency — no runtime impact until beans are wired
2. Deploy new Flyway migration (`payments` table, `checkout_session_id` on `bookings`) — additive, no breaking changes
3. Deploy new `payment` module and updated `booking` module together
4. Configure `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`, `STRIPE_SUCCESS_URL`, `STRIPE_CANCEL_URL` in environment
5. Register webhook endpoint in Stripe Dashboard: `POST /api/v1/webhooks/stripe`, events: `checkout.session.completed`, `checkout.session.expired`
6. Smoke test with Stripe test mode keys
7. Remove old `ExpireHoldsJob` (or disable — it will find no bookings with `paymentDeadline` set if column is dropped)

**Rollback**: Feature flag on `CheckoutSessionPort` — if Stripe is unreachable, fall back to returning a booking without `checkoutUrl` (manual payment reference flow). Database migration is additive and safe to leave in place.

## Open Questions

- Should `STRIPE_CANCEL_URL` redirect to a frontend page, or is the backend redirect endpoint sufficient?
- Is 30-minute reservation window acceptable to the business, or do we need to revisit the backend-timer approach?
- Should we support Stripe test/live mode switching via config profile (`dev` vs `prod`)?
