# Design

## Testing Strategy

All new tests follow existing project conventions:

- **Domain model tests**: Plain JUnit 5, `@Nested` groups by method, `@DisplayName` with arrow notation (e.g. "HELD → CONFIRMED")
- **Value object tests**: Plain JUnit 5, no mocks, test validation boundaries
- **Use case tests**: `@ExtendWith(MockitoExtension.class)`, `@Mock` dependencies, AssertJ assertions, `@DisplayName`
- **No inline comments** — self-documenting test method names
- **Framework**: JUnit 5 + Mockito + AssertJ
- **Format**: Palantir Java Format (AOSP style) via Spotless

## Test Structure

```
src/test/java/.../
├── booking/
│   ├── domain/model/BookingTest.java          ← NEW
│   └── application/usecase/
│       ├── CancelBookingUseCaseTest.java      ← NEW
│       └── ExpireHeldBookingsUseCaseTest.java  ← NEW
├── payment/
│   ├── domain/model/PaymentTest.java          ← NEW
│   └── application/usecase/
│       ├── HandlePaymentSuccessUseCaseTest.java          ← NEW
│       ├── HandlePaymentFailedByPaymentIntentUseCaseTest.java ← NEW
│       ├── CancelPendingPaymentUseCaseTest.java          ← NEW
│       ├── RefundPaymentUseCaseTest.java                 ← NEW
│       ├── ExpireCheckoutSessionUseCaseTest.java         ← NEW
│       ├── GetPaymentByBookingIdUseCaseTest.java         ← NEW
│       └── GetPaymentByIdUseCaseTest.java                ← NEW
├── user/
│   ├── domain/model/UserTest.java             ← NEW
│   └── application/usecase/
│       ├── RegisterUserUseCaseTest.java        ← NEW
│       └── LoginUserUseCaseTest.java           ← NEW
└── shared/domain/
    ├── MoneyTest.java                         ← NEW
    ├── EmailAddressTest.java                  ← NEW
    ├── PersonNameTest.java                    ← NEW
    ├── PhoneNumberTest.java                   ← NEW
    └── ResultTest.java                        ← NEW
```
