# Tasks

## 1. Booking Module: Add confirmHeldSeats Port Method

- [x] 1.1 Add `confirmHeldSeats(BookingId bookingId)` method to `RouteSeatAvailabilityPort` interface
- [x] 1.2 Implement `confirmHeldSeats()` in `RouteSeatAvailabilityPortAdapter` — query seats by `bookingId` where `status = HELD`, call `domain.confirm()` to transition `HELD → BOOKED`, save all
- [x] 1.3 Add `confirm()` method to `RouteSeatAvailability` domain model transitioning `HELD → BOOKED`
- [x] 1.4 Export `RouteSeatAvailabilityPort` via `@NamedInterface("ports")` in `train/application/port/package-info.java` so the `payment` module can access it

## 2. Payment Domain Layer

- [x] 2.1 Create `Payment.java` aggregate root with fields: `PaymentId`, `BookingId`, `UserId`, `Money amount`, `PaymentStatus status`, `String checkoutSessionId`, `String stripePaymentIntentId`, `String stripeEventId`, `String errorMessage`; factory methods `create()` and `reconstitute()`
- [x] 2.2 Create `PaymentId.java` — UUID wrapper value object
- [x] 2.3 Create `PaymentStatus.java` enum: `PENDING`, `PAID`, `CANCELLED`, `FAILED`, `REFUNDED`
- [x] 2.4 Create `PaymentRepository.java` port interface with methods: `save(Payment)`, `findByBookingId(BookingId)`, `findByCheckoutSessionId(String)`, `findByStripeEventId(String)`
- [x] 2.5 Create `PaymentError.java` sealed interface with cases: `PaymentNotFound`, `AlreadyProcessed`, `RefundFailed`
- [x] 2.6 Create `PaymentCompleted.java` and `PaymentRefunded.java` domain events

## 3. Payment Application Layer — Ports & Commands

- [x] 3.1 Create `StripeGatewayPort.java` interface with methods: `createCheckoutSession(CreateCheckoutSessionCommand)`, `expireCheckoutSession(String sessionId)`, `createRefund(String paymentIntentId, String idempotencyKey)`
- [x] 3.2 Create `CreateCheckoutSessionCommand.java` record: `bookingId`, `userId`, `amount`, `currency`, `successUrl`, `cancelUrl`
- [x] 3.3 Create `PaymentDto.java` record: `paymentId`, `bookingId`, `status`, `checkoutUrl`, `amount`, `currency`

## 4. Payment Application Layer — Use Cases

- [x] 4.1 Create `CreateCheckoutSessionUseCase.java` — check idempotency (skip if Payment already exists for bookingId), call `stripeGatewayPort.createCheckoutSession()`, save Payment with `status = PENDING`
- [x] 4.2 Create `HandlePaymentSuccessUseCase.java` — check `stripeEventId` idempotency, load booking, if `CANCELLED` issue immediate refund and return, else call `booking.confirm()`, call `confirmHeldSeats(bookingId)`, update Payment to `PAID` with `stripePaymentIntentId`
- [x] 4.3 Create `RefundPaymentUseCase.java` — find Payment by bookingId where `status = PAID`, call `stripeGatewayPort.createRefund(paymentIntentId, "refund_{bookingId}")`, update Payment to `REFUNDED`; on Stripe exception log error and do NOT rethrow (booking cancellation must not be blocked)
- [x] 4.4 Create `ExpireCheckoutSessionUseCase.java` — find Payment by bookingId where `status = PENDING`, call `stripeGatewayPort.expireCheckoutSession(checkoutSessionId)`, update Payment to `CANCELLED`
- [x] 4.5 Create `GetPaymentUseCase.java` — find Payment by bookingId, verify requesting userId matches booking owner, return `PaymentDto`

## 5. Payment Application Layer — Event Listeners

- [x] 5.1 Create `OnBookingCreatedListener.java` — `@ApplicationModuleListener` on `BookingCreated` event, delegates to `CreateCheckoutSessionUseCase`
- [x] 5.2 Create `OnBookingCancelledListener.java` — `@ApplicationModuleListener` on `BookingCancelled` event, delegates to `RefundPaymentUseCase` if `requiresRefund = true`, else calls `ExpireCheckoutSessionUseCase`
- [x] 5.3 Create `OnBookingExpiredListener.java` — `@ApplicationModuleListener` on booking expiry event (or reuse `BookingCancelled` with `requiresRefund = false`), delegates to `ExpireCheckoutSessionUseCase`

## 6. Payment Infrastructure — Stripe Adapter

- [x] 6.1 Create `StripeGatewayAdapter.java` implementing `StripeGatewayPort` — inject `Stripe` client from config, implement `createCheckoutSession()` using `SessionCreateParams` with `expires_at = now + 1800s`, `payment_method_types = [card]`, metadata `{bookingId, userId}`
- [x] 6.2 Implement `expireCheckoutSession()` in `StripeGatewayAdapter` — call `Session.expire(sessionId)`; handle `InvalidRequestException` (already expired) gracefully
- [x] 6.3 Implement `createRefund()` in `StripeGatewayAdapter` — call `Refund.create()` with `payment_intent`, `reason = customer_request`, and idempotency key header
- [x] 6.4 Create `StripeConfig.java` `@Configuration` — read `stripe.api-key`, `stripe.webhook-secret`, `stripe.success-url`, `stripe.cancel-url` from `application.yaml`; initialize `Stripe.apiKey`

## 7. Payment Infrastructure — Persistence

- [x] 7.1 Create `PaymentEntity.java` `@Entity` mapping to `payments` table with all columns: `id`, `booking_id`, `checkout_session_id`, `stripe_event_id`, `amount`, `currency`, `status`, `stripe_payment_intent_id`, `error_message`, `created_at`, `updated_at`
- [x] 7.2 Create `PaymentJpaRepository.java` Spring Data interface with: `findByBookingId`, `findByCheckoutSessionId`, `findByStripeEventId`
- [x] 7.3 Create `PaymentEntityMapper.java` mapping `PaymentEntity ↔ Payment` domain model
- [x] 7.4 Create `PaymentRepositoryAdapter.java` implementing `PaymentRepository` port

## 8. Payment Infrastructure — Web

- [x] 8.1 Create `StripeWebhookController.java` at `POST /api/v1/webhooks/stripe` — permit without auth in security config, read raw body, verify signature via `Webhook.constructEvent()`, dispatch to use cases based on event type: `checkout.session.completed`, `checkout.session.expired`, `payment_intent.payment_failed`
- [x] 8.2 Create `PaymentController.java` at `GET /api/v1/payments/{bookingId}` — authenticated, delegates to `GetPaymentUseCase`, returns `JsendResponse<PaymentHttpResponse>`
- [x] 8.3 Create `PaymentHttpResponse.java` record: `paymentId`, `bookingId`, `status`, `checkoutUrl`, `amount`, `currency`
- [x] 8.4 Enable raw body in `NestFactory` / Spring Boot main — add `spring.mvc.pathmatch` config and ensure `HttpServletRequest` raw body is accessible for webhook signature verification (use `ContentCachingRequestWrapper` or configure `RawBodyRequestFilter`)

## 9. Module Wiring

- [x] 9.1 Create `payment/package-info.java` with `@ApplicationModule(allowedDependencies = {"booking::events", "train::ports"})`
- [x] 9.2 Ensure `booking/domain/event/package-info.java` exports events via `@NamedInterface("events")`
- [x] 9.3 Add `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`, `STRIPE_SUCCESS_URL`, `STRIPE_CANCEL_URL` to `.env.example` and deployment docs
- [x] 9.4 Add `/api/v1/webhooks/stripe` to the security permit-all list (no JWT required for webhook endpoint)

## 10. Tests

- [x] 10.1 `PaymentTest.java` — unit test `Payment.create()`, status transitions, domain event registration
- [x] 10.2 `CreateCheckoutSessionUseCaseTest.java` — mock `StripeGatewayPort` and `PaymentRepository`; verify idempotency skip, session creation, Payment saved as PENDING
- [x] 10.3 `HandlePaymentSuccessUseCaseTest.java` — test happy path (HELD → CONFIRMED + seats BOOKED), test late payment path (CANCELLED booking → immediate refund)
- [x] 10.4 `RefundPaymentUseCaseTest.java` — test refund issued with correct idempotency key; test Stripe exception is swallowed and logged
- [x] 10.5 `StripeWebhookControllerTest.java` (`@WebMvcTest`) — test invalid signature returns 400; test `checkout.session.completed` dispatches to correct use case
- [x] 10.6 `PaymentRepositoryAdapterTest.java` (`@DataJpaTest`) — test `findByBookingId`, `findByCheckoutSessionId`, `findByStripeEventId`
- [x] 10.7 `PaymentModuleTest.java` (`@ApplicationModuleTest`) — verify Spring Modulith module boundaries are respected
