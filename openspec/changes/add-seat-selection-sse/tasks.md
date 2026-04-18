# Tasks

## 1. SSE hook foundation

- [x] 1.1 Add TypeScript types for seat SSE payloads, event names, and
      connection-state values in
      `frontend/customer/src/lib/hooks/use-seat-sse.ts`
- [x] 1.2 Implement authenticated SSE connection logic with `fetch`,
      `ReadableStream`, frame parsing, and unmount cleanup in `useSeatSSE`
- [x] 1.3 Implement reconnection scheduling with exponential backoff from 1s up
      to 30s and reset behavior after reconnect ← (verify: retries follow
      1s/2s/4s progression, cap at 30s, and stop after cleanup)

## 2. Seat-selection page integration

- [x] 2.1 Integrate `useSeatSSE` into
      `frontend/customer/src/components/seats/seat-selection.tsx` while
      preserving `getCoachSeatMap` as the initial data source
- [x] 2.2 Merge `seat-initial` and `seat-changed` updates into the rendered
      coach seat map and reconcile invalidated selected seats
- [x] 2.3 Add a localized connected/reconnecting status indicator to the
      seat-selection UI ← (verify: live seat changes update the grid and summary
      without manual refetch, and connection state is visible during reconnects)

## 3. Localization and test coverage

- [x] 3.1 Add English and Vietnamese translation keys for seat SSE
      connection-status messages in `frontend/customer/src/messages/en.json` and
      `vi.json`
- [x] 3.2 Add unit tests for
      `frontend/customer/src/lib/hooks/use-seat-sse.test.ts` covering stream
      parsing, merge behavior, reconnect backoff, and cleanup ← (verify: tests
      simulate `seat-initial`/`seat-changed` chunks, retry timing, and unmount
      abort behavior)
