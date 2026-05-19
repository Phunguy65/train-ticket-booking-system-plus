## ADDED Requirements

### Requirement: Trip card displays date below time
Each trip card SHALL display the date (dd/MM/yyyy format) below both the departure time and arrival time, using a smaller muted style to maintain the time-first visual hierarchy.

#### Scenario: Departure time with date
- **WHEN** a trip card renders with a valid `departureTime`
- **THEN** the departure time SHALL display as large bold text (HH:mm)
- **AND** the departure date SHALL display directly below in smaller muted text (dd/MM/yyyy)

#### Scenario: Arrival time with date
- **WHEN** a trip card renders with a valid `arrivalTime`
- **THEN** the arrival time SHALL display as large bold text (HH:mm)
- **AND** the arrival date SHALL display directly below in smaller muted text (dd/MM/yyyy)

#### Scenario: Missing departure or arrival time
- **WHEN** a trip card renders with a null/undefined `departureTime` or `arrivalTime`
- **THEN** the time SHALL display as "-"
- **AND** no date line SHALL be rendered below it
