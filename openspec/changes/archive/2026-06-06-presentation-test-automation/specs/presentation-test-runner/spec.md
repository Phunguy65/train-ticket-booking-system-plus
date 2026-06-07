## ADDED Requirements

### Requirement: Test scenario button on each UC slide
Each use case slide (UC-01 through UC-12) SHALL display a "Chạy kiểm thử" button that opens the test execution popup.

#### Scenario: Button visible on UC slide
- **WHEN** user navigates to any UC slide (slides 3-14)
- **THEN** a "Chạy kiểm thử" button is visible in the slide area

#### Scenario: Button opens popup overlay
- **WHEN** user clicks the "Chạy kiểm thử" button on a UC slide
- **THEN** a full-screen popup overlay appears with the test execution interface for that specific UC

### Requirement: Popup overlay with tabbed scenarios
The popup SHALL display 3 tabs representing the 3 test scenario types, an iframe for the live app, and a log panel.

#### Scenario: Popup layout
- **WHEN** the popup opens for any UC
- **THEN** it SHALL contain: a header with UC name, 3 tabs ("Thành công", "Lỗi Validation", "Lỗi Business"), an iframe area, and a log panel below

#### Scenario: Tab switching
- **WHEN** user clicks a different tab
- **THEN** the iframe reloads with the appropriate page and the automation runs the selected scenario

#### Scenario: Close popup
- **WHEN** user clicks the close button or presses Escape
- **THEN** the popup closes and returns to the slide view

### Requirement: Same-origin iframe loading
The iframe SHALL load pages from the same origin (localhost:3000) to enable DOM scripting.

#### Scenario: iframe loads target page
- **WHEN** a scenario tab is selected
- **THEN** the iframe navigates to the corresponding frontend route (e.g., `/vi/register` for UC-01)

#### Scenario: Server unreachable
- **WHEN** the popup opens but the frontend server is not running
- **THEN** the popup displays an error message indicating the server is unreachable

### Requirement: React form automation engine
The automation engine SHALL fill form inputs using native value setters and dispatch synthetic events compatible with React-hook-form.

#### Scenario: Fill text input
- **WHEN** the automation sets a value on a text/email/password input
- **THEN** it uses the native HTMLInputElement value setter and dispatches an `input` event with `bubbles: true`

#### Scenario: Submit form
- **WHEN** all fields are filled
- **THEN** the automation clicks the submit button and waits for the response to render

#### Scenario: Wait for React hydration
- **WHEN** the iframe page loads
- **THEN** the automation waits until the target form inputs are present in the DOM before starting (via polling or MutationObserver, max 5 seconds timeout)

### Requirement: Pre-authentication for protected routes
For UCs requiring authentication (UC-03 through UC-12), the automation SHALL authenticate via API before loading the target page.

#### Scenario: Login before protected page
- **WHEN** a scenario requires authentication
- **THEN** the automation calls POST `/api/auth/login` with test credentials, stores the token in the iframe's localStorage, then navigates to the target page

#### Scenario: Auth failure handling
- **WHEN** the login API call fails
- **THEN** the log panel displays the authentication error and the scenario is aborted

### Requirement: Three scenarios per UC
Each UC SHALL have exactly 3 defined scenarios with specific test data.

#### Scenario: UC-01 Register scenarios
- **WHEN** UC-01 popup opens
- **THEN** 3 scenarios are available: (1) successful registration with valid data, (2) validation error with invalid email format, (3) business logic error with duplicate email

#### Scenario: UC-02 Login scenarios
- **WHEN** UC-02 popup opens
- **THEN** 3 scenarios are available: (1) successful login with valid credentials, (2) validation error with empty password, (3) business logic error with wrong credentials

#### Scenario: All UCs covered
- **WHEN** any UC popup (UC-01 through UC-12) opens
- **THEN** all 3 tabs have defined scenario data with appropriate test inputs and expected outcomes

### Requirement: Execution log panel
The popup SHALL display a real-time log of test execution steps and API responses.

#### Scenario: Log shows automation steps
- **WHEN** a scenario is running
- **THEN** the log panel displays each step: "Navigating to...", "Filling field X...", "Submitting form...", "Response received: ..."

#### Scenario: Log shows API response
- **WHEN** the form submission completes
- **THEN** the log panel shows the HTTP status code and response body (or error message displayed on the UI)

### Requirement: Visual feedback during automation
The automation SHALL provide visual indication of progress.

#### Scenario: Step-by-step animation
- **WHEN** the automation fills each field
- **THEN** there is a visible delay (300-500ms) between actions so the audience can follow the progression

#### Scenario: Success/error indication
- **WHEN** the scenario execution completes
- **THEN** the log panel shows a green checkmark for expected outcomes or a red X for unexpected failures
