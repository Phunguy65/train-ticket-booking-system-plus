## 1. i18n Keys

- [x] 1.1 Add `Trips.backToHome` key to `frontend/customer/src/messages/en.json` with value "Back to Home"
- [x] 1.2 Add `Trips.backToHome` key to `frontend/customer/src/messages/vi.json` with value "Về trang chủ"

## 2. Back to Home Button

- [x] 2.1 Add `HomeIcon` import and `Link` import to `search-results.tsx`
- [x] 2.2 Add a "Back to Home" button (outline variant, sm size, with HomeIcon) to the left side of the header row in the success results view of `SearchResults` component
- [x] 2.3 Ensure the back button is also visible in the empty results state and error state ← (verify: button appears in all render states — results, empty, error)

## 3. Trip Card Date Display

- [x] 3.1 Import `formatShortDate` in `trip-card.tsx`
- [x] 3.2 Add date line (`text-xs text-muted-foreground`) below departure time, rendered only when `departureTime` is defined
- [x] 3.3 Add date line (`text-xs text-muted-foreground`) below arrival time, rendered only when `arrivalTime` is defined ← (verify: date displays correctly for both departure and arrival, not rendered when time is null)

## 4. Verification

- [x] 4.1 Run frontend build to confirm no type or compilation errors ← (verify: build passes cleanly with no warnings related to these changes)
