# ADDED Requirements

## Requirement: Homepage trip search form

The system SHALL replace the current marketing-style homepage body with a
customer trip search form that lets users choose origin station, destination
station, and departure date before navigating to localized search results.

### Scenario: Search from homepage with valid criteria

- **WHEN** the user selects an origin station, selects a destination station,
  picks a departure date, and submits the form
- **THEN** the system navigates to `/[locale]/search` with query parameters for
  origin, destination, and departure date

### Scenario: Swap origin and destination

- **WHEN** the user activates the swap control on the homepage form
- **THEN** the system exchanges the selected origin and destination values
  without clearing the chosen departure date

### Scenario: Prevent invalid same-station search

- **WHEN** the user attempts to submit the form with the same station selected
  for both origin and destination, or with missing required fields
- **THEN** the system shows inline validation errors and does not navigate to
  the search page

## Requirement: Station autocomplete inputs

The system SHALL provide origin and destination combobox inputs backed by the
`searchStations` API so users can search stations by typing and selecting from
localized suggestions.

### Scenario: Load station suggestions while typing

- **WHEN** the user types into the origin or destination station field
- **THEN** the system queries `searchStations` and presents matching stations in
  an accessible combobox list

### Scenario: Select a station from suggestions

- **WHEN** the user chooses a station suggestion by keyboard or pointer
- **THEN** the system stores the selected station identifier and renders the
  selected station label in the field

### Scenario: Station lookup fails

- **WHEN** the station autocomplete request fails because of a network or API
  error
- **THEN** the system shows toast-based error feedback and allows the user to
  retry the lookup by interacting with the field again

## Requirement: Search results list

The system SHALL render a trip search results page at `/[locale]/search` using
`filterScheduledTrips` and display each matching trip with train, schedule,
route, price, duration, and seat availability information.

### Scenario: Render matching trips

- **WHEN** the search results query returns matching trips
- **THEN** the system renders a results list where each trip shows train name,
  departure time, arrival time, duration, origin, destination, price, and
  available seat count

### Scenario: Render empty state when no trips match

- **WHEN** the search results query completes with zero matching trips
- **THEN** the system renders a localized empty state explaining that no trips
  were found for the selected criteria

### Scenario: Preserve search criteria in the results view

- **WHEN** the user opens or refreshes the search results page with valid query
  parameters
- **THEN** the system rehydrates the form and results state from the URL query
  parameters and re-runs the trip query

## Requirement: Search sorting and filtering

The system SHALL allow users to refine search results by sorting and filtering
supported by the `filterScheduledTrips` API.

### Scenario: Sort by supported field

- **WHEN** the user changes the selected sort option to departure time, price,
  or duration
- **THEN** the system re-queries `filterScheduledTrips` with the selected sort
  mode and reorders the rendered results accordingly

### Scenario: Filter by price range

- **WHEN** the user applies a minimum and/or maximum price filter
- **THEN** the system re-queries `filterScheduledTrips` with the selected price
  range and renders only matching trips

### Scenario: Filter to available seats only

- **WHEN** the user enables the available-seats-only filter
- **THEN** the system excludes trips with no available seats from the rendered
  results

## Requirement: Cursor-based infinite pagination

The system SHALL support cursor-based infinite scrolling for trip results and
continue loading additional pages until the API reports no more data.

### Scenario: Load next result page on scroll trigger

- **WHEN** the user reaches the end of the currently rendered results and the
  current query has a next cursor
- **THEN** the system requests the next page from `filterScheduledTrips` and
  appends the new trips to the existing list

### Scenario: Stop pagination when no more pages exist

- **WHEN** the current search query reports that there is no next page
- **THEN** the system stops requesting additional pages and does not show a
  further loading trigger

## Requirement: Loading and recoverable error states for search

The system SHALL provide localized skeleton loading, retryable network error
states, and non-blocking feedback during trip search interactions.

### Scenario: Show skeletons during initial results load

- **WHEN** the search results page is waiting for the first
  `filterScheduledTrips` response
- **THEN** the system renders skeleton placeholders instead of blank content

### Scenario: Retry after results request failure

- **WHEN** the trip results request fails because of a network or transient API
  error
- **THEN** the system renders an error state with a retry action that triggers
  the same search request again
