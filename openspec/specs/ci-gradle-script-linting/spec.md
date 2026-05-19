# ADDED Requirements

## Requirement: Detect Gradle script changes in CI

The CI workflow SHALL compute a dedicated `gradle` change signal in `detect-changes` using path filters that include `build.gradle.kts`, `settings.gradle.kts`, `build-logic/**`, and `gradle.properties`.

### Scenario: Gradle script file changed

-  **WHEN** a pull request modifies at least one configured Gradle script path
-  **THEN** `detect-changes` marks the `gradle` filter as changed

### Scenario: No Gradle script file changed

-  **WHEN** a pull request does not modify any configured Gradle script path
-  **THEN** `detect-changes` marks the `gradle` filter as not changed

## Requirement: Run dedicated Gradle script lint job

The CI workflow SHALL run a `lint-gradle` job only when the `gradle` filter is changed, and the job SHALL set up Java 25 (Temurin), configure `gradle/actions/setup-gradle@v4`, and execute `./gradlew ktlintCheck`.

### Scenario: Gradle changes trigger lint-gradle

-  **WHEN** `detect-changes` reports `gradle` as changed
-  **THEN** the `lint-gradle` job runs and executes `./gradlew ktlintCheck` with required tool setup

### Scenario: No Gradle changes skip lint-gradle

-  **WHEN** `detect-changes` reports `gradle` as not changed
-  **THEN** the `lint-gradle` job is skipped

## Requirement: Include lint-gradle in CI summary gate

The CI summary job SHALL include `lint-gradle` in its `needs` list and SHALL evaluate its result in summary status logic.

### Scenario: lint-gradle failure fails summary

-  **WHEN** `lint-gradle` completes with a failure state
-  **THEN** `ci-summary` reports failure for the workflow

### Scenario: lint-gradle success preserves pass state

-  **WHEN** all required jobs including `lint-gradle` succeed or are appropriately skipped
-  **THEN** `ci-summary` reports success
