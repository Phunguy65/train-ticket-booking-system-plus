# ADDED Requirements

## Requirement: Release-please workflow on main

The repository SHALL include a GitHub Actions workflow at `.github/workflows/release-please.yml` that triggers on pushes to `main`, uses `googleapis/release-please-action@v4` in manifest mode, and grants `contents: write` and `pull-requests: write` permissions.

### Scenario: Push to main triggers release-please

-  **WHEN** commits are pushed to `main`
-  **THEN** the release-please workflow runs in manifest mode

### Scenario: Workflow has required permissions

-  **WHEN** release-please attempts to open or update release PRs
-  **THEN** workflow permissions allow repository content updates and pull request writes

## Requirement: Manifest-based multi-component release configuration

The repository SHALL include `release-please-config.json` and `.release-please-manifest.json` defining two components: root `.` (backend simple release) and `frontend/customer` (node release), with `separate-pull-requests: true`, `include-component-in-tag: true`, and initial versions `0.1.0` for both components.

### Scenario: Separate release PRs by component

-  **WHEN** releasable conventional commits exist for multiple components
-  **THEN** release-please opens separate release PRs per configured component

### Scenario: Component tags include component names

-  **WHEN** a component release PR is merged
-  **THEN** generated tag format includes component name, such as `backend@v0.1.0` or `customer@v0.1.0`

## Requirement: Changelog and version file updates per component

Release configuration SHALL generate component-specific changelogs (root `CHANGELOG.md` for backend and `frontend/customer/CHANGELOG.md` for customer), SHALL include changelog sections for `feat`, `fix`, `perf`, `docs`, and `chore` with `chore` hidden, and SHALL update root/backend Gradle version files via release-please markers from baseline `0.1.0`.

### Scenario: Backend changelog and gradle versions updated

-  **WHEN** backend component release is prepared
-  **THEN** root `CHANGELOG.md` and configured Gradle version files are updated using release-please version markers

### Scenario: Customer changelog updated

-  **WHEN** customer component release is prepared
-  **THEN** `frontend/customer/CHANGELOG.md` is updated for customer release notes
