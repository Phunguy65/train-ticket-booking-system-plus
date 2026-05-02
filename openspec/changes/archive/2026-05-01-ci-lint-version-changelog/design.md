# Context

This monorepo already runs CI for backend/frontend code paths, uses conventional commits, and maintains separate backend/frontend version metadata. However, Gradle Kotlin DSL script changes are not currently lint-gated in CI, Dependabot references a removed frontend path, and release notes/version bumps are managed manually. The requested change introduces CI coverage for Gradle script linting, corrects dependency automation scope, and establishes release-please manifest-based automation for backend and customer components.

## Goals / Non-Goals

**Goals:**

- Ensure CI detects Gradle script changes and runs `./gradlew ktlintCheck` in a dedicated `lint-gradle` job.
- Ensure CI summary/gating includes `lint-gradle` status so Gradle lint failures are visible and blocking.
- Correct Dependabot configuration to target existing frontend customer package updates.
- Introduce release-please manifest-based automation that:
  - runs on push to `main`,
  - opens component-specific release PRs,
  - updates changelogs per component,
  - bumps versions in root/backend Gradle files and customer package metadata,
  - uses component-prefixed tags.
- Align initial release baseline versions for backend and customer components at `0.1.0`.

**Non-Goals:**

- No modification of backend or frontend application business logic.
- No deployment workflow creation or changes.
- No lint rule changes for Spotless, ktlint rulesets, or Biome.
- No restructuring of existing CI jobs outside required integration points.

## Decisions

1. Add a dedicated Gradle-change filter in `detect-changes`.
- Decision: Extend `.github/workflows/ci.yml` change detection with a `gradle` path filter covering root/build-logic Gradle files.
- Rationale: Avoid running `ktlintCheck` on unrelated changes while ensuring Kotlin Gradle script edits are always validated.
- Alternative considered: Always running `ktlintCheck` on every CI run. Rejected because it increases runtime and duplicates selective-path optimization already used in CI.

2. Add `lint-gradle` as an independent CI job with explicit tooling.
- Decision: New job runs only when `detect-changes` reports Gradle changes, using Java 25 (Temurin) and `gradle/actions/setup-gradle@v4`, then executes `./gradlew ktlintCheck`.
- Rationale: Isolates Gradle-script lint feedback from backend/frontend jobs and matches toolchain requirements.
- Alternative considered: Folding into existing backend lint/test jobs. Rejected because triggers and ownership differ; Gradle-script checks should remain path-scoped and independently observable.

3. Integrate `lint-gradle` into CI summary gates.
- Decision: Update `ci-summary` dependencies and summary script evaluation to include `lint-gradle`.
- Rationale: Ensures required-check behavior and final pass/fail summary remain authoritative.
- Alternative considered: Leaving summary unchanged. Rejected because it could allow silent failures or incomplete reporting.

4. Replace stale Dependabot frontend target with active customer path.
- Decision: Remove `frontend/admin` update entry and add `frontend/customer` npm entry with weekly cadence and `chore(customer)` commit-message prefix.
- Rationale: Keeps automation aligned to current repository structure and commit conventions.
- Alternative considered: Single repo-wide npm entry. Rejected to preserve scoped updates and predictable commit prefixes.

5. Use release-please manifest mode for multi-component release automation.
- Decision: Create `.github/workflows/release-please.yml` with `googleapis/release-please-action@v4`, push trigger on `main`, and permissions `contents: write`, `pull-requests: write`.
- Rationale: Manifest mode supports independent component version tracking/tags/changelogs within monorepo.
- Alternative considered: Single-component or non-manifest configuration. Rejected because backend and customer require separate version flows and changelogs.

6. Configure component metadata and changelog policy in `release-please-config.json`.
- Decision: Use `separate-pull-requests: true`, `include-component-in-tag: true`; define root (`.`) as `simple` package `backend` with extra files (both Gradle build files), and `frontend/customer` as `node` package `customer`; keep chore hidden in changelog sections.
- Rationale: Produces tags like `backend@vX.Y.Z` and `customer@vX.Y.Z`, keeps backend version sourced from Gradle files, and generates concise changelogs focused on user-relevant changes.
- Alternative considered: Combined PR/releases. Rejected due to independent release cadence needs and reduced review clarity.

7. Establish initial manifest and version markers at `0.1.0`.
- Decision: Set `.release-please-manifest.json` to `{ ".": "0.1.0", "frontend/customer": "0.1.0" }`; update root/backend Gradle versions to `0.1.0` with release-please markers.
- Rationale: Aligns backend versioning with existing customer baseline and enables deterministic future automated bumps.
- Alternative considered: Keeping `0.0.1-SNAPSHOT` until first release PR. Rejected because manifest + version files should share a coherent baseline before automation runs.

## Risks / Trade-offs

- [CI job gating mismatch] → Mitigation: Update both `needs` and summary script logic so `lint-gradle` result participates in final CI verdict.
- [Release config drift between manifest and file versions] → Mitigation: Pin initial versions uniformly at `0.1.0` and include both Gradle files in root component `extra-files`.
- [Incorrect tag/changelog behavior due to config syntax] → Mitigation: Keep config minimal, use documented release-please keys, and validate JSON/YAML syntax before merge.
- [Additional CI runtime on Gradle file changes] → Mitigation: Restrict `lint-gradle` execution via path-based detection.

## Migration Plan

1. Merge workflow/config/version-file changes together in one PR.
2. On merge to `main`, CI continues existing paths and additionally runs `lint-gradle` for Gradle script changes.
3. Release-please workflow activates on `main` pushes and opens separate release PRs per configured component.
4. Review generated release PRs for version bumps and changelog entries; merge to cut tags.
5. Rollback strategy: revert release-please workflow/config/manifest and Gradle version-marker edits in a follow-up PR if automation behavior is incorrect.

## Open Questions

- None blocking implementation; requested tag format, component mapping, and version baseline are already specified.
