# Tasks

## 1. Shared UI foundation and route shell

- [x] 1.1 Add the required shadcn/ui primitives (combobox-related
      command/popover, calendar, select, dialog, tabs, badge, skeleton,
      dropdown-menu, sheet, avatar) and ensure they compile within the existing
      radix-nova setup.
- [x] 1.2 Create the shared `(main)` and `(protected)` route layouts plus the
      reusable header, mobile navigation, locale switcher, and optional footer
      shell components.
- [x] 1.3 Wire shared navigation to localized routes and auth-aware rendering
      for signed-out and signed-in states using the authenticated-user query
      pattern. <- (verify: main/protected layouts render consistently, locale
      switching preserves route context, and header states match authenticated
      vs unauthenticated users)

## 2. Localization, validation, and shared customer utilities

- [x] 2.1 Expand `messages/vi.json` and `messages/en.json` with namespaces for
      search, trips, seats, booking, account, profile, navigation, statuses, and
      shared feedback text.
- [x] 2.2 Add shared validation schemas and helper utilities for trip search
      params, profile editing, booking status labels, price/date formatting, and
      seat-selection constraints.
- [x] 2.3 Add reusable query/error helpers for retryable network states,
      toast-based API error handling, and URL search-param parsing/serialization
      across search and booking flows. <- (verify: all new user-facing strings
      are localized in both locales, validation errors resolve through
      translations, and shared helpers support the planned route/query flows)

## 3. Homepage search experience

- [x] 3.1 Replace the current homepage content with a localized trip search form
      that uses react-hook-form + zod, station origin/destination fields, date
      selection, and a swap action.
- [x] 3.2 Implement station autocomplete combobox components backed by
      `searchStations`, including keyboard support, loading states, and
      retryable error feedback.
- [x] 3.3 Submit valid homepage searches to `/[locale]/search` with stable query
      parameters and block invalid searches with inline errors. <- (verify:
      homepage search enforces required fields and same-station validation,
      autocomplete selection works by keyboard and pointer, and submit navigates
      with correct localized query params)

## 4. Search results and infinite pagination

- [x] 4.1 Build the `/search` page with URL-driven query hydration,
      `filterScheduledTrips` integration, and trip cards that render train,
      schedule, route, price, duration, and availability details.
- [x] 4.2 Add sorting and filtering controls for departure time, price,
      duration, price range, and available-seats-only behavior using
      query-backed state.
- [x] 4.3 Implement skeleton loading, empty state, recoverable error state, and
      cursor-based infinite pagination for appended results. <- (verify: results
      rehydrate from the URL on refresh, sorting/filtering update the query and
      rendered data correctly, and pagination appends until no next cursor
      remains)

## 5. Seat selection flow

- [x] 5.1 Build the `/trips/[tripId]/seats` page with `getCoachSeatMap` data
      loading, coach tabs, and recoverable invalid-trip/error states.
- [x] 5.2 Implement the visual seat grid with accessible status presentation for
      available, held, and booked seats plus prevention of non-selectable seat
      activation.
- [x] 5.3 Add multi-seat selection state, selected-seat summary, total price
      calculation, max-five-seat enforcement, and booking handoff to
      `/[locale]/booking`. <- (verify: coach switching updates the rendered seat
      map, unavailable seats cannot be selected, selection is capped at five
      seats, and continue remains disabled until valid seats are chosen)

## 6. Booking confirmation and post-booking detail

- [x] 6.1 Build the `/booking` confirmation page that rehydrates selected
      trip/seat context, loads authenticated passenger info, and blocks
      submission when booking context is incomplete.
- [x] 6.2 Implement booking creation with `createBooking`, pending state
      handling, localized failure recovery, and redirect preparation for the
      checkout URL.
- [x] 6.3 Build the `/booking/[bookingId]` detail page to load `getBooking` and
      display booking status, trip details, seats, passenger info, and payment
      deadline data. <- (verify: booking confirmation shows correct
      trip/seat/passenger summary, createBooking success surfaces confirmation
      metadata before checkout redirect, and booking detail renders the
      persisted booking accurately)

## 7. Protected account dashboard and profile management

- [x] 7.1 Implement the `(protected)` auth gate that redirects unauthenticated
      users to the localized login page before rendering account content.
- [x] 7.2 Build the `/account` dashboard with `getUserBookings`, empty/error
      states, localized status badges, and actions to open booking details.
- [x] 7.3 Add the booking cancellation flow with confirmation dialog,
      `cancelBooking` mutation, query invalidation, and localized success/error
      feedback.
- [x] 7.4 Build the `/account/profile` page with `getAuthenticatedUser`,
      editable profile form fields, `updateAuthenticatedUser`, and inline
      translated validation. <- (verify: protected routes redirect correctly,
      bookings list and badges match API data, cancellation updates dashboard
      state safely, and profile edits save/refetch user data with translated
      validation/errors)

## 8. Automated test coverage for critical customer flows

- [x] 8.1 Add unit tests for shared customer utilities such as search-param
      parsing, booking-status label mapping, price/date formatting, and
      seat-selection rules.
- [x] 8.2 Add component tests for homepage search and profile/booking-related
      forms, covering validation, disabled states, and localized feedback.
- [x] 8.3 Add integration tests for the key customer flows: trip search, seat
      selection to booking handoff, booking creation recovery/success behavior,
      and account cancellation flow. <- (verify: automated tests cover the
      highest-risk search/booking/account paths and fail when core validation,
      localization, or flow wiring regresses)
