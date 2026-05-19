## Why

The search results page (`/search`) lacks a back-to-home navigation button and only displays departure/arrival times without dates. Users cannot easily return to the homepage, and when trips span multiple days or overnight routes, the missing date context causes confusion.

## What Changes

- Add a "Back to Home" button at the top of the search results list for quick navigation back to the homepage.
- Display full date below the departure and arrival times in each trip card (time remains large/bold, date appears as a smaller muted line underneath).

## Capabilities

### New Capabilities

- `search-results-navigation`: Adds a back-to-home button in the search results header area for improved navigation flow.
- `trip-card-datetime-display`: Enhances trip card time display to include the date below the time, using a large-time + small-date layout pattern.

### Modified Capabilities

(none)

## Impact

- `frontend/customer/src/components/trips/search-results.tsx` — add back-to-home button in the results header
- `frontend/customer/src/components/trips/trip-card.tsx` — add date display below departure/arrival times
- `frontend/customer/src/messages/en.json` — add `Trips.backToHome` i18n key
- `frontend/customer/src/messages/vi.json` — add `Trips.backToHome` i18n key
- No backend changes required
- No API contract changes
