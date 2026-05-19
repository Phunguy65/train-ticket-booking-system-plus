# Tasks

## 1. CI Gradle script lint integration

-  [x] 1.1 Update `.github/workflows/ci.yml` `detect-changes` to add a `gradle` filter for `build.gradle.kts`, `settings.gradle.kts`, `build-logic/**`, and `gradle.properties`.
-  [x] 1.2 Add `lint-gradle` job in `.github/workflows/ci.yml` conditioned on the `gradle` filter, with Java 25 (Temurin), `gradle/actions/setup-gradle@v4`, and `./gradlew ktlintCheck`.
-  [x] 1.3 Update `ci-summary` in `.github/workflows/ci.yml` to include `lint-gradle` in both `needs` and summary evaluation script. ← (verify: lint-gradle status is consumed by summary gate and a failing lint-gradle makes ci-summary fail)

## 2. Dependabot configuration correction

-  [x] 2.1 Edit `.github/dependabot.yml` to remove the obsolete `frontend/admin` update entry.
-  [x] 2.2 Add npm ecosystem updates for `frontend/customer` with weekly schedule and commit message prefix `chore(customer)`. ← (verify: dependabot config references only existing frontend path and generated PRs for customer use required prefix)

## 3. Release-please automation setup

-  [x] 3.1 Create `.github/workflows/release-please.yml` to run on push to `main` using `googleapis/release-please-action@v4` in manifest mode with `contents: write` and `pull-requests: write` permissions.
-  [x] 3.2 Create `release-please-config.json` with `separate-pull-requests: true`, `include-component-in-tag: true`, root `.` simple package `backend` with Gradle `extra-files`, and `frontend/customer` node package `customer` with required changelog sections (hide `chore`).
-  [x] 3.3 Create `.release-please-manifest.json` with initial versions for `.` and `frontend/customer` set to `0.1.0`.
-  [x] 3.4 Update `build.gradle.kts` and `backend/build.gradle.kts` versions to `0.1.0` and add release-please version markers. ← (verify: markers exactly wrap version assignment in both files and release-please can bump them)

## 4. Validation and quality gates

-  [x] 4.1 Validate YAML and JSON syntax for `.github/workflows/ci.yml`, `.github/workflows/release-please.yml`, `.github/dependabot.yml`, `release-please-config.json`, and `.release-please-manifest.json`.
-  [x] 4.2 Run `./gradlew ktlintCheck` and ensure it passes.
-  [x] 4.3 Run `./gradlew spotlessCheck` and ensure it still passes. ← (verify: both Gradle checks pass without introducing lint/format regressions)
