# ADDED Requirements

## Requirement: Dependabot customer frontend targeting

Dependabot SHALL define an npm update configuration for `/frontend/customer` and SHALL not define updates for `/frontend/admin`.

### Scenario: Dependabot scans customer frontend

-  **WHEN** Dependabot runs npm update checks
-  **THEN** it scans dependencies under `/frontend/customer`

### Scenario: Dependabot does not scan removed admin frontend

-  **WHEN** Dependabot configuration is evaluated
-  **THEN** no npm update entry exists for `/frontend/admin`

## Requirement: Dependabot schedule and commit prefix

The `/frontend/customer` npm update configuration SHALL use a weekly schedule and SHALL set commit message prefix to `chore(customer)`.

### Scenario: Weekly customer dependency updates

-  **WHEN** Dependabot executes scheduled checks
-  **THEN** customer dependency update PRs are created on a weekly cadence

### Scenario: Customer update commit prefix

-  **WHEN** Dependabot opens a customer dependency update PR
-  **THEN** the generated commit message prefix is `chore(customer)`
