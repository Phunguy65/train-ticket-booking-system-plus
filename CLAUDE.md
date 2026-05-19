# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Multi-platform train ticket booking system: Spring Boot backend, Next.js customer app, PostgreSQL database. The system handles trip search, seat selection with live SSE updates, booking with hold-then-pay flow, and Stripe payments.

## Development Commands

### Backend (Spring Boot)
```bash
cd backend
./gradlew bootRun                          # Start dev server (port 8080)
./gradlew test                             # Run all tests (requires Docker for TestContainers)
./gradlew test --tests '*.BookingTest'     # Run a single test class
./gradlew spotlessApply                    # Format Java code (Palantir AOSP style)
./gradlew spotlessCheck                    # Check formatting
./gradlew exportCustomerOpenApi            # Export OpenAPI YAML to shared/api-contracts/
```

### Frontend Customer App (Next.js)
```bash
cd frontend/customer
bun install                                # Install dependencies
bun run dev                                # Start dev server (port 3000)
bun run build                              # Production build
bun run test                               # Run Vitest tests
bun run lint                               # Biome check (lint + format check)
bun run lint:fix                           # Biome auto-fix
bun run format                             # Biome format
bun run codegen                            # Regenerate API client from OpenAPI spec
```

### Infrastructure
```bash
docker-compose up -d                       # Start PostgreSQL + Valkey + backend + frontend
docker-compose down                        # Stop all services
```

Backend requires `JWT_SECRET` env var. Tests use `application-test.yaml` which auto-configures TestContainers PostgreSQL and uses simple cache (no Redis needed).

## Architecture

### Backend — Clean Architecture by Feature Module

Package root: `io.github.phunguy65.ttbs.backend`

Feature modules: `booking`, `payment`, `station`, `train`, `user`, `shared`

Each module has three layers with enforced dependencies (ArchUnit tests in `architecture/`):
- **domain/** — Pure Java: entities (extend `AggregateRoot<ID>`), value objects, domain events, repository interfaces, errors. No Spring, no JPA.
- **application/** — Use cases (one per operation), command/query DTOs, response DTOs. Spring annotations allowed. Must not depend on infrastructure.
- **infrastructure/** — JPA entities/repos (`{Entity}Entity`, `{Entity}JpaRepository`, `{Entity}RepositoryAdapter`), REST controllers, external service adapters.

Cross-module: direct dependency on another module's domain/application is allowed. Never depend on another module's infrastructure layer.

Key patterns:
- `Result<T, E>` monad instead of exceptions for business errors
- Factory methods: `Entity.create()` registers domain events; `Entity.reconstitute()` does not
- `shared/domain/` contains `AggregateRoot`, `Result`, `Money`, `DomainEvent`, `UuidGenerator`
- `shared/infrastructure/web/` has `JsendResponse` wrapper for all API responses
- External service ports (Stripe, etc.) live in application layer; adapters in `infrastructure/external/`

### Frontend — Next.js 16 + App Router

Runtime: Bun. Linter/Formatter: Biome (not ESLint/Prettier).

Route structure under `src/app/[locale]/`:
- `(auth)/` — login, register (no header/footer)
- `(main)/` — public pages: home, search, trips/seats, booking
- `(protected)/` — requires auth: account, payment, ticket

Key conventions:
- **i18n**: next-intl with `vi` (default) and `en` locales. Messages in `src/messages/{vi,en}.json`. Use `useTranslations('Namespace')` hook.
- **API client**: Auto-generated from `shared/api-contracts/openapi.yaml` via `@hey-api/openapi-ts`. Output in `src/lib/api/generated/` (never edit manually). Produces TypeScript types, Zod schemas, and TanStack React Query hooks.
- **UI components**: shadcn/ui (radix-nova style) in `src/components/ui/` (excluded from Biome linting). Tailwind CSS v4.
- **Import extensions**: Biome enforces explicit `.ts`/`.tsx` extensions on all imports.
- **Path aliases**: `@/` maps to `src/`.

### API Contract Flow

Backend exports OpenAPI spec → `shared/api-contracts/openapi.yaml` → Frontend runs `bun run codegen` → generates typed client with TanStack Query hooks. Keep the contract in sync when changing endpoints.

### Database

PostgreSQL 18 with Flyway migrations in `backend/src/main/resources/db/migration/`. Cache layer uses Valkey (Redis-compatible) for trip/seat queries.

## Formatting & Linting

- **Backend Java**: Spotless with Palantir Java Format (AOSP style). Run `./gradlew spotlessApply` before committing.
- **Gradle/Kotlin scripts**: ktlint via Spotless. 4-space indent, no trailing commas.
- **Frontend**: Biome. Single quotes, JSX single quotes, 4-space indent, trailing commas, 80-char line width.

## OpenSpec Workflow

This project uses OpenSpec for change management. Specs and changes live in `openspec/`. See `openspec/config.yaml` for architecture rules that must be followed when creating new features.
