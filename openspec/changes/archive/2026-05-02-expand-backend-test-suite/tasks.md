# Tasks

## 1. Domain model tests

- [x] 1.1 Create `BookingTest.java`: test create() returns HELD status and registers BookingCreated event; confirm() HELD→CONFIRMED registers BookingConfirmed; cancel() from HELD sets requiresRefund=false; cancel() from CONFIRMED sets requiresRefund=true; cancel() from CANCELLED fails with InvalidStatusTransition; constructor rejects duplicate passenger ID documents
- [x] 1.2 Create `PaymentTest.java`: test create() returns PENDING status; markPaid() sets status/stripePaymentIntentId/stripeEventId and registers PaymentCompleted; markCancelled() sets CANCELLED; markFailed() sets FAILED with errorMessage and stripeEventId; markRefunded() sets REFUNDED and registers PaymentRefunded
- [x] 1.3 Create `UserTest.java`: test create() sets role CUSTOMER and registers UserRegistered; softDelete() sets deletedAt and registers UserDeleted; softDelete() is idempotent (already deleted returns success, no second event); reconstitute() does not register events

## 2. Shared value object tests

- [x] 2.1 Create `MoneyTest.java`: test vnd(long) and vnd(BigDecimal) factory methods; of(BigDecimal, Currency); toLong() returns exact value; toLong() throws ArithmeticException for fractional; equals uses compareTo (new BigDecimal("500000") equals BigDecimal.valueOf(500000)); toString format "amount CURRENCY"
- [x] 2.2 Create `EmailAddressTest.java`: test rejects null (NPE), blank, missing @, starts with @, ends with @; normalizes to lowercase; trims whitespace; valid email accepted
- [x] 2.3 Create `PersonNameTest.java`: test rejects null (NPE), blank, >255 chars; normalizes multi-whitespace to single space; trims leading/trailing whitespace
- [x] 2.4 Create `PhoneNumberTest.java`: test rejects null (NPE), too short (<8 after normalize), too long (>20); normalizes: removes spaces/dashes/parens; preserves + prefix; ofNullable returns null for null and blank
- [x] 2.5 Create `ResultTest.java`: test success(value) isSuccess=true isFailure=false; success() void overload; failure(error) isSuccess=false isFailure=true; map transforms success value; map leaves failure untouched; fold applies onSuccess for success; fold applies onFailure for failure

## 3. Payment use case tests

- [x] 3.1 Create `HandlePaymentSuccessUseCaseTest.java`: happy path confirms booking HELD→CONFIRMED, confirms held seats, marks payment PAID, publishes domain events and SSE event; idempotent when duplicate stripeEventId; late payment after booking CANCELLED triggers refund via stripeGatewayPort and marks REFUNDED
- [x] 3.2 Create `HandlePaymentFailedByPaymentIntentUseCaseTest.java`: happy path finds PENDING payment by paymentIntentId and marks FAILED with errorMessage; idempotent when duplicate stripeEventId; no-op when no payment found; no-op when payment is not PENDING
- [x] 3.3 Create `CancelPendingPaymentUseCaseTest.java`: marks PENDING payment as CANCELLED; no-op when no payment found; no-op when payment is not PENDING
- [x] 3.4 Create `RefundPaymentUseCaseTest.java`: happy path calls stripeGatewayPort.createRefund with idempotencyKey, marks REFUNDED, publishes PaymentRefunded event; no-op when no payment found; no-op when payment is not PAID
- [x] 3.5 Create `ExpireCheckoutSessionUseCaseTest.java`: happy path calls stripeGatewayPort.expireCheckoutSession and marks CANCELLED; no-op when no payment found; no-op when payment is not PENDING
- [x] 3.6 Create `GetPaymentByBookingIdUseCaseTest.java`: delegates to PaymentReadAuthorizer.authorizeAndMap; passes correct bookingId and requestingUserId
- [x] 3.7 Create `GetPaymentByIdUseCaseTest.java`: happy path returns PaymentDetailResponse with booking, trip, seats, passengers; returns PaymentNotFound when payment missing; returns Forbidden when userId mismatch

## 4. User use case tests

- [x] 4.1 Create `RegisterUserUseCaseTest.java`: happy path creates user with CUSTOMER role, saves via repository, publishes UserRegistered event, returns UserResponse; returns EmailAlreadyExists failure when email exists
- [x] 4.2 Create `LoginUserUseCaseTest.java`: happy path finds user by email, verifies password, generates tokens via RefreshTokenManager, returns LoginResultResponse; returns InvalidCredentials when email not found; returns InvalidCredentials when password wrong

## 5. Booking use case tests

- [x] 5.1 Create `CancelBookingUseCaseTest.java`: happy path from HELD cancels booking and releases held seats via releaseHeldSeats, publishes domain events + SSE; happy path from CONFIRMED cancels booking and cancels booked seats via cancelBookedSeats; returns BookingNotFound when booking missing; returns Forbidden when userId mismatch; returns InvalidStatusTransition when already CANCELLED
- [x] 5.2 Create `ExpireHeldBookingsUseCaseTest.java`: expires multiple bookings, releases seats for each, saves all, publishes events + SSE for each; no-op when no expired bookings; skips bookings that fail to cancel and continues processing others

## 6. Final checks

- [x] 6.1 Run `./gradlew test` — all tests pass
- [x] 6.2 Run `./gradlew spotlessCheck` — formatting compliance
