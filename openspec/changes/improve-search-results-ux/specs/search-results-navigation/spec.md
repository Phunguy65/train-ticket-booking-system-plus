## ADDED Requirements

### Requirement: Back to Home button in search results
The search results page SHALL display a "Back to Home" navigation button that allows users to return to the homepage.

#### Scenario: Button is visible when results are displayed
- **WHEN** the search results page renders with trip results
- **THEN** a "Back to Home" button SHALL be visible in the header area (left side, before the results count)

#### Scenario: Button is visible when no results are found
- **WHEN** the search results page renders with zero results
- **THEN** a "Back to Home" button SHALL still be visible in the header area

#### Scenario: Button navigates to homepage
- **WHEN** the user clicks the "Back to Home" button
- **THEN** the application SHALL navigate to the homepage (`/`)

#### Scenario: Button uses localized text
- **WHEN** the locale is Vietnamese
- **THEN** the button text SHALL display "Về trang chủ"
- **WHEN** the locale is English
- **THEN** the button text SHALL display "Back to Home"
