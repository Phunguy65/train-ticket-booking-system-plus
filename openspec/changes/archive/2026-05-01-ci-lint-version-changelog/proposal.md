# Why

The CI pipeline currently does not validate Gradle Kotlin script style changes, Dependabot configuration includes a removed frontend path, and releases/version changelogs are not automated across backend and customer components. This creates avoidable CI regressions and manual release overhead.

## What Changes

- Add Gradle script change detection in CI and introduce a dedicated `lint-gradle` job that runs `./gradlew ktlintCheck` using Java 25 and Gradle setup action.
- Update CI summary aggregation so `lint-gradle` is included in required checks and summary evaluation.
- Fix Dependabot configuration by removing `frontend/admin` and adding `frontend/customer` npm updates with weekly schedule and `chore(customer)` commit prefix.
- Add release-please automation in manifest mode with a new GitHub Actions workflow for pushes to `main`.
- Add root `release-please-config.json` to define separate pull requests, component-aware tags, changelog sections, component package settings, and changelog output locations.
- Add `.release-please-manifest.json` with initial component versions for root (`.`) and `frontend/customer`.
- Update root and backend Gradle version declarations from `0.0.1-SNAPSHOT` to `0.1.0` and add release-please version markers.

## Capabilities

### New Capabilities

- `ci-gradle-script-linting`: Detect Gradle script changes and enforce ktlint checks in CI summary gating.
- `dependabot-customer-updates`: Keep customer frontend npm dependencies updated via Dependabot with valid directory targeting and commit prefixing.
- `automated-multi-component-release-management`: Automatically open release PRs with changelogs and coordinated version bumps for backend and customer components.

### Modified Capabilities

- None.

## Impact

- Affected CI workflows: `.github/workflows/ci.yml` and new `.github/workflows/release-please.yml`.
- Affected dependency automation: `.github/dependabot.yml`.
- Affected release metadata/configuration: `release-please-config.json`, `.release-please-manifest.json`.
- Affected versioned build metadata: `build.gradle.kts`, `backend/build.gradle.kts`.
- No application runtime logic, deployment, or lint rule behavior changes.
