# Context

The customer web application already has the platform foundations needed for a
full booking experience: Next.js 16 App Router, React 19, TailwindCSS 4,
next-intl locale routing, TanStack Query, generated OpenAPI hooks, and a small
set of auth-related pages and shadcn/ui primitives. However, the app currently
stops after login and registration, while the backend already exposes the
customer APIs needed for trip discovery, seat selection, booking creation,
booking management, and authenticated profile access.

This change spans multiple route groups, shared navigation, authenticated and
unauthenticated states, i18n coverage, and critical customer flows that cross
search, seat selection, booking, and account management. The design therefore
needs a coherent approach for route composition, data fetching, mutation/error
handling, and progressive enhancement so the implementation can stay consistent
across the new surface area.

## Goals / Non-Goals

**Goals:**

- Build the complete customer-facing booking journey from homepage search to
  booking confirmation and account management.
- Reuse the generated TanStack Query hooks and the app's existing auth, toast,
  validation, and locale-routing patterns instead of introducing parallel data
  access abstractions.
- Establish a shared main layout with auth-aware navigation that works across
  desktop and mobile breakpoints.
- Ensure all new customer-facing text, statuses, and interaction feedback are
  translated in Vietnamese and English.
- Define an implementation approach that supports accessibility, keyboard
  navigation, loading states, retries, and testability for critical flows.

**Non-Goals:**

- Changing backend API behavior, booking business rules, or seat allocation
  semantics.
- Building payment UI beyond redirecting users to the checkout URL returned by
  the booking flow.
- Introducing a new global state library beyond TanStack Query plus local React
  state.
- Redesigning the existing auth pages except where shared navigation or locale
  behavior must integrate with them.

## Decisions

### 1. Use route groups to separate public shell and protected account areas

The customer app will follow the requested App Router structure with a shared
`(main)` layout for homepage, search, seat-selection, booking, and booking
detail routes, and a `(protected)` layout for account and profile pages. The
top-level locale layout remains the source of next-intl context, while the main
and protected layouts provide route-specific shell behavior.

Why this choice:

- It matches the target route structure without overloading a single layout with
  conflicting responsibilities.
- Shared header and responsive navigation belong in a common shell for the main
  browsing experience.
- Protected account logic can be enforced at the layout boundary instead of
  duplicating auth checks in each page.

Alternatives considered:

- Put all new pages under one shared layout: rejected because protected account
  pages need auth gating and different navigation expectations.
- Protect each account page individually: rejected because it duplicates auth
  checks and increases redirect inconsistency.

### 2. Keep TanStack Query as the single server-state layer, with URL state for search and booking context

Server data will be loaded through generated query and mutation options from the
OpenAPI hooks. Search criteria, sorting, filters, and cursor progression will be
reflected in route query parameters so results are shareable and reload-safe.
Seat selection state stays local to the seat page until the user continues,
after which `/booking` receives the selected `tripId` and `seatIds` through URL
search params and rehydrates the necessary summary data from query cache and/or
detail queries.

Why this choice:

- TanStack Query is already configured and aligns with the generated SDK.
- Search and booking context encoded in the URL supports refresh, navigation,
  and deep-link resilience better than ephemeral in-memory state alone.
- It avoids introducing custom global stores for flows that are fundamentally
  query-driven.

Alternatives considered:

- Store search and booking state in a client-only context provider: rejected
  because refreshes would lose important flow state and debugging becomes
  harder.
- Put all flow state in local component state only: rejected because pagination,
  filters, and booking handoff need stable navigation semantics.

### 3. Build composable feature components around existing form and error-handling patterns

New experiences will be implemented from composable client components such as a
trip search form, trip-results filter bar, trip cards, seat-map widgets, booking
summary panels, bookings tables/cards, and profile forms. All forms use
react-hook-form with zod validation, inline translated error messages, disabled
states during mutation, and sonner toasts for API/network failures using the
same style already established in auth forms.

Why this choice:

- The current auth forms already establish a working pattern for validation,
  translated messages, mutation handling, and navigation.
- Breaking the experience into feature components improves testability and
  prevents route pages from becoming monolithic.
- Reusing the same interaction conventions reduces UX inconsistency.

Alternatives considered:

- Implement each page as a mostly self-contained route component: rejected
  because it would duplicate query, empty-state, and error-state logic.
- Use uncontrolled forms with manual validation: rejected because it conflicts
  with existing react-hook-form usage and makes inline validation harder.

### 4. Add the required shadcn/ui primitives and wrap domain-specific behavior on top of them

The implementation will add the requested shadcn primitives—Combobox, Calendar,
Select, Dialog, Tabs, Badge, Skeleton, DropdownMenu, Sheet, Avatar, Popover, and
Command—and compose them into domain-specific widgets such as station
autocomplete, date picker, coach selector, booking-status badges, and responsive
user navigation.

Why this choice:

- The existing project already uses shadcn/ui with the radix-nova style, so new
  primitives will visually and structurally align with the current design
  system.
- Accessibility behavior for focus trapping, keyboard support, and overlay
  semantics is easier to achieve when starting from established primitives.
- Domain-specific wrappers keep booking logic out of low-level UI components.

Alternatives considered:

- Build custom primitives from scratch: rejected because it adds unnecessary
  accessibility and maintenance risk.
- Use a third-party component library outside shadcn: rejected because it would
  conflict with the existing component approach and styling system.

### 5. Protect account routes at the server layout boundary and expose auth state to shared navigation via user query

The `(protected)` layout will enforce authenticated access and redirect to the
locale-aware login route when the current user is absent. The shared header will
derive its signed-in/signed-out rendering from a lightweight authenticated-user
query so public pages can still display auth-aware actions without blocking the
entire page shell on hard failure.

Why this choice:

- Route protection is more reliable when enforced before rendering protected
  content.
- Shared navigation still needs current-user awareness across public and private
  routes.
- Separating hard protection from soft auth-aware rendering prevents global
  shell failures when an optional user query errors on public pages.

Alternatives considered:

- Perform account auth checks only on the client: rejected because it causes
  flashes of protected UI and weaker redirect behavior.
- Force every page to block on current-user loading: rejected because public
  browsing should remain available even if the auth lookup is slow or fails.

### 6. Model search, seat, and booking steps as recoverable flows with explicit loading, empty, and retry states

Search results use skeleton loading and an empty state when no trips match.
Network failures present a retry action. Seat maps expose clear availability
states and enforce a five-seat client-side selection cap before booking. Booking
creation is the only mutation that commits the reservation and is guarded by a
confirmation page summarizing trip, seats, passenger info, total price, and the
next redirect. Booking cancellation uses a confirmation dialog and mutation
feedback before invalidating bookings queries.

Why this choice:

- The booking journey contains multiple points where API data may be stale,
  missing, or temporarily unavailable.
- Recoverable UI states reduce abandonment and make testing deterministic.
- Explicit mutation boundaries help prevent accidental duplicate bookings or
  destructive actions.

Alternatives considered:

- Submit bookings directly from the seat page: rejected because it skips review
  of passenger and price details.
- Hide failures behind generic fallback UI: rejected because retryable customer
  actions need actionable feedback.

### 7. Treat accessibility and localization as first-class acceptance criteria, not polish work

All new flows will include translated labels, helper text, button copy, status
labels, aria text, and error messages in `vi` and `en`. Components that use
dialogs, popovers, comboboxes, tabs, sheets, and menus will preserve keyboard
navigation, focus management, and screen-reader-readable state. Responsive
behavior is designed as part of the main route shell and feature components
rather than as a final pass.

Why this choice:

- The feature set relies heavily on interactive controls that can become
  inaccessible if localization and keyboard behavior are deferred.
- The app already has locale-aware routing, so untranslated copy would create an
  incomplete experience.
- Accessibility requirements directly influence component selection and form
  structure.

Alternatives considered:

- Add translations after UI implementation: rejected because it tends to leave
  hard-coded strings and broken message structures.
- Treat accessibility as best-effort QA only: rejected because the feature uses
  high-interaction patterns that need explicit implementation decisions.

## Risks / Trade-offs

- [Search and booking context in URL becomes too large or inconsistent] →
  Mitigation: limit URL state to stable identifiers and filters, keep derived UI
  state local, and re-fetch server truth when needed.
- [Seat availability changes between selection and booking confirmation] →
  Mitigation: revalidate seat-related data on booking handoff and surface clear
  error/retry messaging if selection becomes invalid.
- [Shared header auth lookup introduces extra requests on public routes] →
  Mitigation: keep the user query lightweight, cache it through TanStack Query,
  and avoid making the rest of the shell dependent on its success.
- [Infinite scroll becomes hard to use or test accessibly] → Mitigation: pair it
  with explicit loading indicators, deterministic query keys, and a fallback
  "load more" trigger pattern if intersection-only behavior proves brittle.
- [Translation scope expands across many UI states and status labels] →
  Mitigation: organize message namespaces by feature and include translation
  updates in each implementation task group rather than as a final batch.
- [Profile editing requirements are underspecified compared to booking flows] →
  Mitigation: constrain profile work to fields already available through the
  authenticated user contract and reuse existing validation patterns.

## Migration Plan

1. Add the missing shadcn/ui primitives and shared customer shell components.
2. Update locale messages and shared utilities needed by new routes.
3. Implement homepage search and `/search` results flow with cursor pagination.
4. Implement seat selection and `/booking` confirmation/creation flow.
5. Implement protected account, booking-detail, and profile pages.
6. Add and refine automated tests for utilities, forms, and key integration
   flows.
7. Validate responsive behavior, auth redirects, accessibility basics, and i18n
   coverage before release.

Rollback strategy:

- Revert the new route groups, components, message keys, and UI primitives if
  the booking UI is not ready for release.
- Because backend APIs are unchanged, rollback is isolated to the frontend app
  and does not require data migrations.

## Open Questions

- Whether the final implementation should use automatic intersection-triggered
  infinite scrolling only, or also expose an explicit fallback "load more"
  action for accessibility and deterministic testing.
- Whether booking detail under `/booking/[bookingId]` should be reachable only
  after creation or also linked from the account dashboard as an alternate
  detail view alongside the protected bookings list.
