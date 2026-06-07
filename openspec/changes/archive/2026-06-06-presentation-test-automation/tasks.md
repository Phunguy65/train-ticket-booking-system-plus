## 1. Popup Overlay Infrastructure

- [x] 1.1 Add CSS styles for popup overlay, tabs, iframe container, log panel, test button
- [x] 1.2 Add popup HTML structure (overlay, header, tabs, iframe, log panel, close button)
- [x] 1.3 Add popup open/close JavaScript (openTestPopup, closeTestPopup, Escape key handler)

## 2. Automation Engine

- [x] 2.1 Implement React-compatible form automation helpers (setReactInputValue, clickButton, waitForElement)
- [x] 2.2 Implement pre-authentication function (loginViaApi, setTokenInIframe)
- [x] 2.3 Implement scenario execution engine (runScenario: load iframe → wait hydration → execute steps → log results)
- [x] 2.4 Implement log panel rendering (addLogEntry with timestamps, status icons, step descriptions)

## 3. Scenario Definitions

- [x] 3.1 Define UC-01 (Register) scenarios: success, invalid email format, duplicate email
- [x] 3.2 Define UC-02 (Login) scenarios: success, empty password, wrong credentials
- [x] 3.3 Define UC-03 (Logout) scenarios: success, invalid token format, already revoked token
- [x] 3.4 Define UC-04 (Profile) scenarios: success update, invalid phone format, email already used
- [x] 3.5 Define UC-05 (Station search) scenarios: found results, empty query validation, station not found by ID
- [x] 3.6 Define UC-06 (Trip search) scenarios: found trips, invalid date format, no trips found
- [x] 3.7 Define UC-07 (Seat map) scenarios: seats displayed, invalid pagination, trip not found
- [x] 3.8 Define UC-08 (Booking) scenarios: success hold, seats already held, trip not found
- [x] 3.9 Define UC-09 (View bookings) scenarios: list displayed, invalid pagination, booking not found
- [x] 3.10 Define UC-10 (Cancel booking) scenarios: success cancel, already cancelled, booking not owned
- [x] 3.11 Define UC-11 (View payment) scenarios: payment found, payment not found, not owned
- [x] 3.12 Define UC-12 (Payment) scenarios: checkout created, booking not held, already paid ← (verify: all 36 scenarios have complete test data, target URLs, and expected outcomes)

## 4. Slide Integration

- [x] 4.1 Add "Chạy kiểm thử" button to each UC slide (slides 3-14) with data-uc attribute
- [x] 4.2 Wire button click handlers to openTestPopup with correct UC number
- [x] 4.3 Implement tab switching logic (load scenario on tab click) ← (verify: all 12 slides have working buttons, popup opens correctly, tabs switch scenarios, automation executes for UC-01 and UC-02 at minimum)

## 5. Deployment Setup

- [x] 5.1 Copy presentation.html to frontend/customer/public/presentation.html
- [x] 5.2 Update slide-images paths in the public copy to reference correct relative paths
