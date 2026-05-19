# Tasks

## 1. Reusable booking flow UI primitives

- [x] 1.1 Create `frontend/customer/src/components/ui/stepper.tsx` with desktop
      and mobile variants, accessible step semantics, and support for completed,
      current, and future step states
- [x] 1.2 Create `frontend/customer/src/components/ui/sticky-footer.tsx` as a
      mobile-only sticky CTA container aligned with the existing design system
- [x] 1.3 Add or update shared exports for the new UI primitives in
      `frontend/customer/src/components/ui/index.ts` ← (verify: stepper and
      sticky footer import cleanly from shared UI exports and render correctly
      on mobile vs desktop breakpoints)

## 2. Booking-specific component composition

- [x] 2.1 Create `frontend/customer/src/components/booking/booking-stepper.tsx`
      with localized `Search`, `Seats`, `Review`, and `Payment` labels plus
      backward-only navigation behavior
- [x] 2.2 Create
      `frontend/customer/src/components/booking/booking-summary-card.tsx` using
      a mobile-collapsible and desktop-expanded trip summary layout
- [x] 2.3 Create `frontend/customer/src/components/booking/price-breakdown.tsx`
      for itemized seat pricing, optional service fee, and total presentation
- [x] 2.4 Create `frontend/customer/src/components/booking/payment-status.tsx`
      with `PENDING`, `REDIRECTING`, `SUCCESS`, `FAILED`, and `EXPIRED` states,
      countdown handling, and optional retry/start-over actions
- [x] 2.5 Update `frontend/customer/src/components/booking/index.ts` to export
      the new booking components ← (verify: booking summary, price breakdown,
      and payment status all share consistent props/contracts and the countdown
      turns urgent below five minutes)

## 3. Seat selection and booking page integration

- [x] 3.1 Update `frontend/customer/src/components/seats/seat-selection.tsx` to
      render `BookingStepper` at the seat-selection step and add the mobile
      sticky footer summary/continue CTA without regressing the current desktop
      summary
- [x] 3.2 Refactor
      `frontend/customer/src/components/booking/booking-confirmation.tsx` to use
      `BookingStepper`, `BookingSummaryCard`, and `PriceBreakdown` in place of
      the current inline review layout
- [x] 3.3 Integrate `PaymentStatus` into
      `frontend/customer/src/components/booking/booking-confirmation.tsx` for
      post-booking pending, redirecting, success, failure, and expiry messaging
- [x] 3.4 Update `frontend/customer/src/components/booking/booking-detail.tsx`
      to surface `PaymentStatus` and prominent countdown guidance for `HELD`
      bookings with payment still available ← (verify: seat selection, booking
      review, and booking detail all show the correct current step/state and the
      payment UI behaves consistently across confirmation and detail pages)

## 4. Localization and verification coverage

- [x] 4.1 Add the new `Stepper`, `Payment`, and `PriceBreakdown` translation
      keys to `frontend/customer/src/messages/en.json`
- [x] 4.2 Add the corresponding translated keys to
      `frontend/customer/src/messages/vi.json`
- [x] 4.3 Add unit tests for the new reusable UI and booking components,
      including stepper state rendering, sticky footer visibility,
      price-breakdown totals, and payment-status countdown/state transitions
- [x] 4.4 Add or update an integration test covering the customer booking flow
      from seat selection through booking confirmation/payment handoff with
      mocked APIs ← (verify: both locales render the new copy, component tests
      cover countdown/status transitions, and the end-to-end booking flow shows
      progress, sticky CTAs, and payment messaging as specified)
