# ADDED Requirements

## Requirement: Localized seat-stream connection messages

The system SHALL provide seat-selection SSE connection-status messages in each
supported locale.

### Scenario: English connection messages

- **WHEN** locale is `en` and the seat-selection page shows SSE connection state
- **THEN** the connection-status labels for connected and reconnecting are read
  from `messages/en.json`

### Scenario: Vietnamese connection messages

- **WHEN** locale is `vi` and the seat-selection page shows SSE connection state
- **THEN** the connection-status labels for connected and reconnecting are read
  from `messages/vi.json`
