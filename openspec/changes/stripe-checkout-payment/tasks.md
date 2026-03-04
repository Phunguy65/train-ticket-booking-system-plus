# Tasks

## 1. Foundation

- [x] 1.1 Add `com.stripe:stripe-java:25.+` dependency to `backend/build.gradle.kts`
- [x] 1.2 Add `StripeProperties` `@ConfigurationProperties` class (`stripe.api-key`, `stripe.webhook-secret`, `stripe.success-url`, `stripe.cancel-url`)
- [x] 1.3 Add Stripe environment variable placeholders to `application.yaml` (`STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`, `STRIPE_SUCCESS_URL`, `STRIPE_CANCEL_URL`)
- [x] 1.4 Write Flyway migration `V{n}__add_stripe_payments.sql`: create `payments` table, add `checkout_session_id` column to `bookings`, add indexes

## 2. Payment Module — Domain

- [x] 2.1 Create `payment/` Spring Modulith module with `package-info.java` (`@ApplicationModule`)
- [x] 2.2 Create `PaymentId` type-safe value object (wraps UUID)
- [x] 2.3 Create `PaymentStatus` enum: `PENDING`, `PAID`, `CANCELLED`
- [x] 2.4 Create `CheckoutSessionId` value object (wraps String)
- [x] 2.5 Create `CheckoutSessionStatus` enum: `OPEN`, `COMPLETE`, `EXPIRED`
- [x] 2.6 Create `Payment` aggregate with `create()`, `confirm()`, `cancel()` methods returning `Result<Void, PaymentError>`; `create()` registers `PaymentCreated` event
- [x] 2.7 Create `PaymentError` sealed interface with `AlreadyProcessed` variant
- [x] 2.8 Create `PaymentRepository` port interface in `payment/domain/repository/`
- [x] 2.9 Create domain events: `PaymentCreated`, `PaymentConfirmed`, `PaymentCancelled`

## 3. Payment Module — Application Port

- [x] 3.1 Create `CheckoutSessionPort` interface in `payment/application/port/` with methods: `createSession(...)`, `expireSession(CheckoutSessionId)`, `retrieveSession(CheckoutSessionId)`
- [x] 3.2 Create `CreateCheckoutSessionCommand` DTO (bookingId, amountVnd, idempotencyKey)
- [x] 3.3 Create `CheckoutSessionDto` output DTO (checkoutSessionId, checkoutUrl, expiresAt)

## 4. Payment Module — Use Cases

- [x] 4.1 Create `ConfirmBookingOnPaymentUseCase`: find payment by `checkoutSessionId`, call `payment.confirm()`, call `ConfirmSeatHoldUseCase` (cross-module via `@NamedInterface`)
- [x] 4.2 Create `CancelBookingOnExpiryUseCase`: find payment by `checkoutSessionId`, call `payment.cancel()`, call `CancelBookingUseCase` (cross-module)
- [x] 4.3 Create `ExpireCheckoutSessionOnCancelListener`: `@ApplicationModuleListener` on `BookingCancelled` event, calls `CheckoutSessionPort.expireSession()`

## 5. Payment Module — Infrastructure: Stripe Adapter

- [x] 5.1 Create `StripeCheckoutAdapter` implementing `CheckoutSessionPort`; configure `Stripe.apiKey` from `StripeProperties` in `@PostConstruct`
- [x] 5.2 Implement `createSession()`: build `SessionCreateParams` with `mode=PAYMENT`, `currency=vnd`, zero-decimal amount, `expires_at=NOW+30min`, `metadata.booking_id`, `after_expiration.recovery.enabled=false`, pass idempotency key via `RequestOptions`
- [x] 5.3 Implement `expireSession()`: call `Session.expire(sessionId, RequestOptions)`
- [x] 5.4 Implement `retrieveSession()`: call `Session.retrieve(sessionId)` and map `status` string to `CheckoutSessionStatus` enum
- [x] 5.5 Create `StripeAmountConverter` utility: `toStripeAmount(BigDecimal vnd)` returns `vnd.longValue()` (no ×100 for zero-decimal currency)

## 6. Payment Module — Infrastructure: Persistence

- [x] 6.1 Create `PaymentEntity` JPA entity mapping to `payments` table
- [x] 6.2 Create `PaymentJpaRepository` Spring Data interface with `findByCheckoutSessionId(String)` and `findByBookingId(UUID)` queries
- [x] 6.3 Create `PaymentRepositoryAdapter` implementing `PaymentRepository` domain port
- [x] 6.4 Create `PaymentEntityMapper` for `PaymentEntity` ↔ `Payment` domain model

## 7. Payment Module — Infrastructure: Webhook Controller

- [x] 7.1 Create `StripeWebhookController` at `POST /api/v1/webhooks/stripe`; configure endpoint to consume raw bytes (`HttpMessageConverter` for `application/json` → `byte[]`)
- [x] 7.2 Implement Stripe signature verification using `Webhook.constructEvent(payload, sigHeader, webhookSecret)`; return `400` on `SignatureVerificationException`
- [x] 7.3 Route `checkout.session.completed` (with `payment_status=paid`) to `ConfirmBookingOnPaymentUseCase`
- [x] 7.4 Route `checkout.session.expired` to `CancelBookingOnExpiryUseCase`
- [x] 7.5 Return `200 OK` for unhandled event types (forward-compatibility)

## 8. Payment Module — Infrastructure: Reconciliation Job

- [x] 8.1 Create `PaymentReconciliationJob` with `@Scheduled(fixedDelay = 300_000)` (5 minutes)
- [x] 8.2 Query `HELD` bookings with `checkout_session_id IS NOT NULL` and `created_at < NOW - 35 minutes`
- [x] 8.3 For each stale booking, call `CheckoutSessionPort.retrieveSession()` and dispatch to `ConfirmBookingOnPaymentUseCase` or `CancelBookingOnExpiryUseCase` based on status

## 9. Booking Module — Domain Changes

- [x] 9.1 Add `checkoutSessionId` field to `Booking` aggregate
- [x] 9.2 Update `Booking.create()` factory signature to accept `checkoutSessionId`; change initial status from `PENDING` to `HELD`; register `BookingHeld` event (rename from `BookingCreated` if needed)
- [x] 9.3 Update `BookingCancelled` domain event to carry `checkoutSessionId`
- [x] 9.4 Update `BookingStatus` enum: rename `PENDING` → `HELD` (verify no other code depends on `PENDING`)
- [x] 9.5 Update `BookingError` to replace `CannotConfirm` / add `AlreadyCancelled` variants per spec

## 10. Booking Module — Application Changes

- [x] 10.1 Update `CreateSeatHoldUseCase`: inject `CheckoutSessionPort`, call `createSession()` after booking is persisted within the same transaction, store returned `checkoutSessionId` on the booking, include `checkoutUrl` and `expiresAt` in `HoldDto`
- [x] 10.2 Update `HoldDto` to include `checkoutUrl: String`, `checkoutSessionId: String`, `expiresAt: Instant`
- [x] 10.3 Update `CreateSeatHoldHttpResponse` to expose `checkoutUrl` and `expiresAt` to the frontend
- [x] 10.4 Update `CancelBookingUseCase` to ensure `BookingCancelled` event carries `checkoutSessionId`
- [x] 10.5 Export `ConfirmSeatHoldUseCase` and `CancelBookingUseCase` via `@NamedInterface("api")` in `booking/application/usecase/package-info.java` for cross-module access from `payment`

## 11. Booking Module — cancel_url Redirect Endpoint

- [x] 11.1 Add `GET /api/v1/bookings/{id}/cancel-redirect` endpoint to `BookingController`; this endpoint calls `CancelBookingUseCase` and returns a `302` redirect to the frontend cancel page URL (from `StripeProperties.cancelUrl`)

## 12. Database

- [x] 12.1 Update `BookingEntity` to add `checkoutSessionId` column mapping
- [x] 12.2 Update `BookingEntityMapper` to map `checkoutSessionId` field
- [x] 12.3 Add `findByCheckoutSessionId` query to `BookingJpaRepository` (needed by reconciliation job)
- [x] 12.4 Remove or repurpose `ExpireHoldsJob` — disable `paymentDeadline`-based polling (the field can remain nullable in DB for backward compat)

## 13. Tests

- [x] 13.1 Unit test `Payment` aggregate: `create()`, `confirm()`, `cancel()`, idempotent confirm
- [x] 13.2 Unit test `StripeAmountConverter`: VND zero-decimal, no ×100
- [x] 13.3 Unit test `ConfirmBookingOnPaymentUseCase` with mocked ports
- [x] 13.4 Unit test `CancelBookingOnExpiryUseCase` with mocked ports
- [x] 13.5 Unit test `ExpireCheckoutSessionOnCancelListener`: session expired on `BookingCancelled`, graceful handling of missing session ID
- [x] 13.6 `@WebMvcTest` for `StripeWebhookController`: valid signature routes correctly, invalid signature returns 400, duplicate event is idempotent
- [x] 13.7 `@DataJpaTest` for `PaymentRepositoryAdapter`: `findByCheckoutSessionId`, `findByBookingId`
- [x] 13.8 `@ApplicationModuleTest` for `payment` module: verify Spring Modulith module boundaries
- [x] 13.9 Update `CreateSeatHoldUseCaseTest`: mock `CheckoutSessionPort`, assert `checkoutUrl` in response, assert rollback on Stripe failure
- [x] 13.10 Update `BookingTest` (domain unit test): verify `HELD` initial status, `checkoutSessionId` on aggregate, `BookingCancelled` carries session ID
