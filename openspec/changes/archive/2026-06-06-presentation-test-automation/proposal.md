## Why

The academic presentation (`docs/presentation.html`) demonstrates 12 use cases but lacks interactive test execution. Adding live test-scenario buttons allows presenters to run real API-backed demonstrations directly from each UC slide, making the QA course demo more compelling and verifiable.

## What Changes

- Add a popup overlay system to `docs/presentation.html` with iframe + automation engine
- Each UC slide (slides 3-14) gains a "Chạy kiểm thử" button
- Clicking the button opens a full-screen popup containing:
  - 3 tabs: Success / Validation Error / Business Logic Error
  - An iframe loading the corresponding app page (same-origin via Next.js public serving)
  - JavaScript automation that fills forms and triggers submissions using React-compatible native value setters
  - A log panel showing request/response activity
- Copy/symlink the presentation file into `frontend/customer/public/` so it serves at `localhost:3000/presentation.html` (same-origin requirement for iframe scripting)

## Capabilities

### New Capabilities
- `presentation-test-runner`: Interactive test scenario execution system embedded in the presentation slides — popup overlay, iframe automation engine, scenario definitions for all 12 UCs (36 total scenarios)

### Modified Capabilities

## Impact

- `docs/presentation.html` — major addition of CSS, HTML overlay, and JS automation engine (~500+ lines)
- `frontend/customer/public/` — new file `presentation.html` (copy or symlink from docs/)
- No backend or frontend source code changes
- No API contract changes
- Requires both backend (port 8080) and frontend (port 3000) running for live demo
