# ADDED Requirements

## Requirement: Shared customer header

The system SHALL provide a shared header for customer-facing pages in the main
layout with navigation that adapts to authentication state.

### Scenario: Unauthenticated header actions

- **WHEN** no authenticated user is available
- **THEN** the header displays the logo or home link and localized login and
  register actions

### Scenario: Authenticated header actions

- **WHEN** an authenticated user is available
- **THEN** the header displays the logo or home link, locale switcher, and a
  user menu with links for My Bookings, Profile, and Logout

## Requirement: Locale switcher in shared navigation

The system SHALL provide a locale switcher in shared navigation that preserves
the current route context when changing between Vietnamese and English.

### Scenario: Change locale from shared header

- **WHEN** the user changes the selected language from the header
- **THEN** the system navigates to the equivalent localized route and updates
  visible UI text to the chosen language

## Requirement: Mobile navigation support

The system SHALL provide a mobile-responsive navigation pattern for the shared
header.

### Scenario: Open mobile navigation sheet

- **WHEN** the user opens navigation on a small viewport
- **THEN** the system displays the available primary navigation and auth/user
  actions in an accessible sheet or equivalent mobile menu

## Requirement: Shared main layout shell

The system SHALL provide a shared main layout wrapper for customer browsing and
booking pages.

### Scenario: Render shared shell around main pages

- **WHEN** the user visits homepage, search, seat-selection, booking, or booking
  detail pages
- **THEN** the page is rendered within the shared main layout with consistent
  header spacing and navigation structure

## Requirement: Optional footer support

The system SHALL allow the shared customer layout to include a simple footer
without interfering with the primary booking flow.

### Scenario: Render simple footer when configured

- **WHEN** the shared main layout includes a footer
- **THEN** the footer displays below primary page content and remains localized
