## Context

The customer-facing search results page (`/search`) renders a list of scheduled trips using `SearchResults` and `TripCard` components. Currently, the page has no explicit navigation back to the homepage (users rely on the header logo), and trip cards display only the time (HH:mm) without the date, which is insufficient for overnight or multi-day searches.

Existing utilities `formatTime` and `formatShortDate` in `customer-utils.ts` already provide the formatting needed. The i18n infrastructure uses `next-intl` with JSON message files per locale (en, vi).

## Goals / Non-Goals

**Goals:**
- Provide a visible "Back to Home" button in the search results area
- Display the full date below departure and arrival times in each trip card
- Maintain existing visual hierarchy (time remains prominent, date is secondary)

**Non-Goals:**
- Changing the search form or filter logic
- Modifying backend API responses
- Adding new date/time formatting utilities (existing ones suffice)
- Changing other pages or components

## Decisions

1. **Back button placement**: Place the button in the header row of `SearchResults` (left side, before the results count). This keeps it visible regardless of scroll position within the results area.
   - Alternative: Place in `search/page.tsx` above `SearchResults` — rejected because it would not benefit from the same loading/error state awareness.

2. **Button style**: Use `Button variant='outline' size='sm'` with a `HomeIcon` from lucide-react, wrapped in a `Link` to `/`. This matches the existing pattern in `payment/success/page.tsx`.

3. **Date display format**: Use `formatShortDate()` (dd/MM/yyyy) rendered as a `<p className='text-xs text-muted-foreground'>` below the existing time `<p>`. This preserves the time-first visual hierarchy while adding date context.

4. **i18n key**: Add `Trips.backToHome` to both locale files. Reuse the same wording as `PaymentSuccess.backToHome` for consistency (en: "Back to Home", vi: "Về trang chủ").

## Risks / Trade-offs

- [Minor layout shift on mobile] → The additional date line adds ~16px height per time block. Acceptable given the information value gained.
- [No risk of breaking existing functionality] → Changes are purely additive UI modifications with no logic changes.
