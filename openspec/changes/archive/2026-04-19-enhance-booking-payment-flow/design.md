# Context

The customer frontend already supports the functional booking path from search
to seat selection, booking confirmation, booking detail, and external payment
checkout. The current implementation renders page-local summaries and actions,
but it does not provide a consistent sense of progress across steps, does not
keep mobile CTAs visible during long pages, and exposes payment timing only as
static text after booking creation or inside booking detail.

This change is contained to `frontend/customer`, but it spans shared UI
primitives, booking-specific components, localized messaging, page composition,
and client-side timer behavior. The design must fit existing Next.js App Router,
`next-intl`, shadcn/ui, Radix, Tailwind v4, and TanStack Query usage patterns
without introducing new backend contracts or state persistence mechanisms.

## Goals / Non-Goals

**Goals:**

- Introduce reusable UI building blocks for booking progress, sticky mobile
  actions, collapsible trip summaries, price breakdowns, and payment-state
  feedback.
- Keep the existing booking/search URL flow and backend payment redirect model
  intact while making the flow easier to understand.
- Surface payment deadlines as live countdowns in booking confirmation/detail
  experiences when the booking is still awaiting payment.
- Preserve current responsive patterns by using mobile-first enhancements that
  do not disrupt desktop layouts.
- Add localized, testable behavior for the new components and the updated
  booking journey.

**Non-Goals:**

- Changing backend booking, payment, or checkout APIs.
- Replacing the existing route sequence or adding new booking pages.
- Introducing session storage, local storage, or a new client-side flow store.
- Redesigning unrelated search, account, or authentication screens.

## Decisions

### 1. Build the enhancement from reusable primitives plus booking-specific wrappers

The shared `Stepper` and `StickyFooter` components will live under
`src/components/ui`, while booking-specific composition (`BookingStepper`,
`BookingSummaryCard`, `PriceBreakdown`, `PaymentStatus`) will live under
`src/components/booking`.

- **Why this approach:** the stepper and sticky-footer patterns can be reused by
  other customer journeys later, while the booking-specific wrappers keep route
  logic, labels, and trip/payment semantics out of low-level UI components.
- **Alternative considered:** implement all enhancements directly inside
  existing page components. Rejected because it would duplicate layout logic,
  blur responsibilities, and make testing individual states harder.

### 2. Keep step state route-derived instead of introducing global flow state

Each page will render a `BookingStepper` with a fixed `currentStep` derived from
its place in the route flow (`Search → Seats → Review → Payment`). Backward
navigation can be enabled where the prior route/context is valid, but forward
navigation remains blocked unless the user is already on that step.

- **Why this approach:** the current flow is already encoded in routes and query
  context, so a route-derived stepper avoids new synchronization concerns.
- **Alternative considered:** maintain a dedicated booking wizard state store.
  Rejected because it adds complexity without solving an existing persistence
  problem and conflicts with the explicit decision to rely on URL context.

### 3. Preserve current desktop summaries while layering a mobile sticky CTA shell

Desktop layouts will continue to render inline action buttons and summary cards.
On mobile, `StickyFooter` will mirror the primary CTA and the most important
price context so users can continue without scrolling back to the action area.

- **Why this approach:** it improves mobile usability while minimizing desktop
  regression risk and preserving existing information density.
- **Alternative considered:** move all actions into a fixed footer on every
  breakpoint. Rejected because it would compete with existing desktop layouts
  and reduce flexibility for page-level content.

### 4. Model payment presentation as an explicit frontend state machine

`PaymentStatus` will accept a constrained set of UI states (`PENDING`,
`REDIRECTING`, `SUCCESS`, `FAILED`, `EXPIRED`) and render the correct
iconography, copy, CTA behavior, and deadline countdown per state. The booking
confirmation page will map booking creation and payment-query outcomes into
these states, and booking detail will map held bookings with a payment deadline
into `PENDING` or `EXPIRED`.

- **Why this approach:** payment handling is the highest-friction part of the
  journey, and an explicit state model keeps page logic predictable and
  testable.
- **Alternative considered:** keep inline conditional fragments in each page.
  Rejected because that duplicates status rendering and increases the chance
  that countdown/expiry behavior diverges between confirmation and detail pages.

### 5. Implement countdowns as client-side derived time remaining from a deadline prop

The countdown timer will compute remaining time from `paymentDeadline` on the
client with a 1-second interval and switch to destructive styling when fewer
than five minutes remain. Expiry will be derived from time reaching zero rather
than waiting for a separate backend push mechanism.

- **Why this approach:** it matches the existing frontend-only scope, gives
  users immediate urgency cues, and does not require new API polling.
- **Alternative considered:** poll the booking endpoint to refresh status every
  few seconds. Rejected because it adds avoidable network cost and is
  unnecessary for a presentation-level countdown.

### 6. Use existing i18n namespaces with new subtrees for flow-specific labels

New translations will be added to the customer locale catalogs under dedicated
`Stepper`, `Payment`, and `PriceBreakdown` keys while reusing existing `Booking`
content where appropriate.

- **Why this approach:** it aligns with current `next-intl` usage and keeps new
  copy discoverable by concern.
- **Alternative considered:** place all new text under `Booking`. Rejected
  because generic components such as stepper and payment status would become too
  tightly coupled to one page namespace.

## Risks / Trade-offs

- **[Risk] Countdown timers can drift slightly from backend truth because they
  are client-derived** → **Mitigation:** derive from the server-provided
  deadline, clamp at zero, and treat the timer as UX guidance while preserving
  backend status as the source of truth.
- **[Risk] Mobile sticky CTA can overlap existing safe areas or bottom
  navigation** → **Mitigation:** use design-system spacing, border/background
  tokens, and page padding that accounts for footer height where integrated.
- **[Risk] Stepper click behavior can confuse users if it appears fully
  navigable** → **Mitigation:** expose only backward navigation as interactive
  and present future steps as non-clickable muted states.
- **[Risk] Payment confirmation states can diverge between booking confirmation
  and booking detail pages** → **Mitigation:** centralize rendering and
  countdown behavior inside `PaymentStatus` and keep page-level code responsible
  only for mapping API/context state into the shared UI states.
- **[Risk] Collapsible mobile summaries may hide important trip context** →
  **Mitigation:** show critical summary headers when collapsed and keep desktop
  always expanded.

## Migration Plan

1. Add the reusable UI primitives (`Stepper`, `StickyFooter`) and booking
   components (`BookingStepper`, `BookingSummaryCard`, `PriceBreakdown`,
   `PaymentStatus`) with exports and unit coverage.
2. Update seat selection to render the booking stepper plus mobile sticky CTA
   while preserving the current desktop summary.
3. Refactor booking confirmation to compose the new summary, pricing, sticky
   CTA, and payment-status experiences around the existing booking
   mutation/payment query flow.
4. Update booking detail to surface the shared payment-status presentation for
   held bookings and prominently show the countdown/deadline.
5. Add English/Vietnamese messages and integration coverage for the updated
   flow.
6. Roll back, if needed, by removing the new components from the affected pages;
   the existing booking flow can continue with its current inline summaries and
   payment messaging.

## Open Questions

- The change assumes the current booking/payment APIs already expose enough
  status and deadline data to distinguish `FAILED` versus `EXPIRED` states in
  the UI; implementation should confirm the exact status mapping in the customer
  API models.
- The booking confirmation flow currently opens payment with an external
  checkout URL; implementation should confirm whether the intended "2-second
  redirecting" experience is automatic navigation, delayed button emphasis, or
  both so tests can assert the correct behavior.
