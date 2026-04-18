## [2026-04-18] Round 1 (from apply auto-verify)

### Code Quality Fixes

- Fixed: Booking confirmation success state now properly shows payment redirect
  status with loading indicator and auto-redirect after 2s to checkout URL
- Fixed: Added i18n keys `Profile.selectDate` and `Profile.selectGender` in
  en.json and vi.json, updated profile-form.tsx to use them
- Fixed: Added i18n keys `Booking.detail.total`, `Booking.detail.retry`,
  `Booking.detail.backToBookings`, `Booking.redirectingToPayment`,
  `Booking.loadingPayment` in both locales
- Fixed: Updated booking-detail.tsx to use localized strings for "Total",
  "Retry", and "Back to My Bookings"
- Fixed: Updated booking-confirmation.tsx success state to use
  `t('detail.backToBookings')` instead of semantically incorrect
  `t('backToSearch')`

### Architecture/Pattern Fixes

- Fixed: Station combobox error toast moved from render-time to useEffect to
  avoid repeated toasts across rerenders
- Fixed: Station combobox retry button now uses `tCommon('retry')` instead of
  incorrect `tErrors('unknownError')`
- Fixed: Seat selection `useMemo` side effect changed to proper `useEffect` for
  setting active coach

### Test Coverage Fixes

- Fixed: Added component tests for TripSearchForm (5 tests) - validates fields
  render, shows validation errors, has swap button
- Fixed: Added component tests for ProfileForm (4 tests) - validates fields
  render, shows save button, validates fullName error
- Fixed: Added component tests for BookingsList (5 tests) - renders prices,
  shows status badges, shows view/cancel buttons, opens cancel dialog
- Fixed: Added integration tests for customer flows (17 tests) - search
  validation, seat selection limits, booking URL handoff, profile validation,
  end-to-end journey

### Remaining Known Issues

- WARNING: Protected account routing uses client-side auth gate instead of
  server-boundary (design mismatch, but functional)
- IntlError warnings in test output for missing
  `Validation.Invalid input: expected date, received Date` key (test still
  passes, cosmetic issue in test environment)
