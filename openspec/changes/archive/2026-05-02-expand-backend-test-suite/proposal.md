# Proposal: Expand Backend Test Suite

## Summary

The backend has 37 test files but major coverage gaps in domain models, the user module (0 tests), and payment use cases (2/9 tested). This change adds ~18 new unit test files covering domain state machines, shared value objects, and missing use case tests — prioritized by business risk.

## Motivation

- Domain models (Booking, Payment, User) contain critical state machine logic with no direct tests
- The user module (authentication, registration) has zero test coverage
- Payment webhook handlers (HandlePaymentSuccess, RefundPayment) are untested despite being the most error-prone paths
- Shared value objects with validation logic (Money, EmailAddress, PhoneNumber) lack boundary tests

## Scope

**In scope:**
- Domain model unit tests (Booking, Payment, User state machines)
- Shared value object tests (Money, EmailAddress, PersonName, PhoneNumber, Result)
- Payment use case tests (7 missing)
- User use case tests (Register, Login)
- Booking use case tests (CancelBooking, ExpireHeldBookings)

**Out of scope:**
- No production code changes
- Station use cases (simple CRUD, low risk)
- Train use cases (12 get/search, mostly delegation)
- New integration tests (focus on unit coverage first)
- Controller tests for auth (lower priority)

## Checks

- `./gradlew test` — all tests pass
- `./gradlew spotlessCheck` — formatting compliance
