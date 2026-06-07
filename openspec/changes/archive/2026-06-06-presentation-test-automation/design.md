## Context

This change adds interactive test automation to an existing academic HTML presentation file. The presentation runs as a standalone HTML file served from the Next.js `public/` folder at `localhost:3000/presentation.html`, ensuring same-origin access to the running frontend app.

## Goals / Non-Goals

**Goals:**
- Embed test scenario execution buttons in each UC slide (12 slides)
- Provide 3 test scenarios per UC (success, validation error, business logic error)
- Automate form interactions via iframe scripting (same-origin)
- Show real-time test execution with request/response logging
- Work reliably during live academic presentation demos

**Non-Goals:**
- No changes to frontend or backend source code
- No test framework integration (this is presentation-only)
- No offline/mock mode (requires running servers)
- No mobile responsiveness for the popup (desktop presentation only)
- No persistent test data management

## Decisions

### 1. React Form Automation via Native Value Setter

**Choice**: Use `Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set` + `dispatchEvent(new Event('input', { bubbles: true }))` to set React-controlled input values.

**Rationale**: React-hook-form (used in the frontend) does not respond to direct `.value` assignment. The native setter triggers React's synthetic event system. This is the standard approach for automating React forms from outside the React tree.

**Alternatives considered**:
- Direct `.value` assignment — does not work with React controlled inputs
- Dispatching KeyboardEvent per character — too slow for demo, unreliable
- Modifying frontend code to accept postMessage — violates "no source changes" constraint

### 2. Pre-login via API for Auth-required UC Demos

**Choice**: For UCs requiring authentication (UC-03 through UC-12), the automation engine first calls `POST /api/auth/login` via `fetch()`, stores the token in the iframe's `localStorage`, then navigates the iframe to the target page.

**Rationale**: Same-origin iframe allows access to `iframe.contentWindow.localStorage`. This avoids automating the login form every time and keeps scenario focus on the actual UC under test.

**Alternatives considered**:
- Automate login form each time — adds 3-5s delay per scenario, distracts from actual UC demo
- Hardcode a long-lived token — tokens expire (15min JWT), unreliable for presentations

### 3. Popup Overlay Architecture

**Choice**: Single reusable popup `<div>` with dynamically swapped iframe `src` and scenario configuration. Tabs switch between scenarios without reopening the popup.

**Rationale**: Keeps DOM minimal (1 popup element reused for all 12 UCs), reduces HTML bloat, allows smooth tab switching during demo.

### 4. Scenario Data as JS Object Map

**Choice**: Define all 36 scenarios in a single `SCENARIOS` JavaScript object keyed by UC number and scenario type, containing: target URL, form field selectors + values, expected outcome description, and automation steps.

**Rationale**: Centralized data makes it easy to maintain and update scenarios. The automation engine reads from this map and executes generically.

### 5. File Serving Strategy

**Choice**: Maintain the canonical file at `docs/presentation.html`. Add a build/copy step note — the file must be copied to `frontend/customer/public/presentation.html` for same-origin serving.

**Rationale**: Keeping source of truth in `docs/` matches existing project structure. The public copy is a deployment artifact.

## Risks / Trade-offs

- [Risk] Backend/frontend not running during presentation → automation fails silently
  → Mitigation: Add a connection check on popup open; show clear error if servers unreachable
- [Risk] Test data not seeded (e.g., no existing user for "email exists" scenario)
  → Mitigation: Document required test data; scenarios use well-known test accounts
- [Risk] React hydration timing — iframe loads but React hasn't mounted yet
  → Mitigation: Wait for specific DOM elements (e.g., form inputs) via MutationObserver or polling before starting automation
- [Risk] Presentation file grows significantly (~500+ lines added)
  → Mitigation: Acceptable for a self-contained presentation demo; no external JS dependencies needed beyond what already exists
