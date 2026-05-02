# Train Ticket Booking System Plus

A full-stack train ticket booking platform with real-time seat availability, hold-then-pay booking flow, and Stripe payment integration. Built with Spring Boot, Next.js, and PostgreSQL.

## Features

- **Trip Search** — search trains between stations by date, with cached results via Valkey
- **Live Seat Selection** — real-time seat availability updates via Server-Sent Events (SSE)
- **Hold-then-Pay Booking** — reserve seats temporarily, then complete payment within a time window
- **Stripe Payments** — secure checkout with success/cancel handling and webhook confirmation
- **User Accounts** — JWT authentication with registration, login, payment history, and printable tickets
- **Internationalization** — Vietnamese (default) and English via next-intl
- **OpenAPI Contract** — auto-generated TypeScript client with TanStack Query hooks

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 25, Spring Boot 4.0, Spring Security, Hibernate ORM 7.2 |
| Frontend | Next.js 16, React 19, Bun, TypeScript, Tailwind CSS v4, shadcn/ui |
| Database | PostgreSQL 18, Flyway migrations |
| Cache | Valkey 8.0 (Redis-compatible) |
| Payments | Stripe |
| API Contract | SpringDoc OpenAPI + @hey-api/openapi-ts codegen |
| CI | GitHub Actions (lint, test, contract check, Docker validation) |
| Code Quality | Spotless (Java), Biome (TypeScript), ktlint (Gradle scripts), Lefthook pre-commit hooks |

## Quick Start

### Prerequisites

- Java 25 (Temurin recommended)
- [Bun](https://bun.sh/) 1.3+
- [Docker](https://docs.docker.com/get-docker/) and Docker Compose
- [pnpm](https://pnpm.io/) 10+

### Using Docker Compose (recommended)

```bash
cp .env.example .env
# Edit .env with your JWT_SECRET and Stripe keys

docker-compose up -d
```

The application will be available at:

- **Customer App**: http://localhost:3000
- **Backend API**: http://localhost:8080
- **API Docs**: http://localhost:8080/swagger-ui.html

### Local Development

**1. Start infrastructure services:**

```bash
docker-compose up -d postgres valkey
```

**2. Start the backend:**

```bash
cp .env.example .env
# Edit .env with your JWT_SECRET (minimum 32 characters)

cd backend
./gradlew bootRun
```

**3. Start the frontend:**

```bash
cd frontend/customer
bun install
bun run dev
```

### Seed Data

Generate seed data for development:

```bash
cd scripts/generate_seed_data
# See README in this directory for usage
```

## Project Structure

```
.
├── backend/                    # Spring Boot API (Java 25)
│   └── src/main/java/.../
│       ├── booking/            # Booking module (hold → pay flow)
│       ├── payment/            # Stripe payment integration
│       ├── station/            # Station management
│       ├── train/              # Trains, trips, seats, SSE
│       ├── user/               # Auth, registration, profiles
│       └── shared/             # AggregateRoot, Result monad, Money
├── frontend/
│   └── customer/               # Next.js 16 customer-facing app
│       └── src/
│           ├── app/[locale]/   # i18n routes (vi, en)
│           │   ├── (auth)/     # Login, register
│           │   ├── (main)/     # Search, trips, booking
│           │   └── (protected)/ # Account, payment, tickets
│           ├── components/     # UI components (shadcn/ui)
│           └── lib/api/        # Auto-generated API client
├── shared/
│   └── api-contracts/          # OpenAPI spec (source of truth)
├── build-logic/                # Gradle convention plugins
├── scripts/                    # Dev utilities and seed data
└── docker-compose.yml
```

### Backend Architecture

The backend follows **Clean Architecture by Feature Module**. Each module (`booking`, `payment`, `station`, `train`, `user`) has three layers:

- **domain/** — pure Java entities, value objects, repository interfaces, domain events
- **application/** — use cases, commands, queries, response DTOs
- **infrastructure/** — JPA persistence, REST controllers, external service adapters

Architectural rules are enforced via ArchUnit tests.

## Development

### Backend Commands

```bash
cd backend
./gradlew bootRun                          # Start dev server (port 8080)
./gradlew test                             # Run all tests (requires Docker)
./gradlew test --tests '*.BookingTest'     # Run a single test class
./gradlew spotlessApply                    # Format Java code
./gradlew spotlessCheck                    # Check formatting
./gradlew exportCustomerOpenApi            # Export OpenAPI spec
```

### Frontend Commands

```bash
cd frontend/customer
bun install                                # Install dependencies
bun run dev                                # Start dev server (port 3000)
bun run build                              # Production build
bun run test                               # Run Vitest tests
bun run lint                               # Biome check (lint + format)
bun run lint:fix                           # Biome auto-fix
bun run codegen                            # Regenerate API client from OpenAPI spec
```

### API Contract Workflow

When backend endpoints change:

```bash
pnpm run customer:sdk:generate
```

This exports the OpenAPI spec from the backend and regenerates the typed frontend client with TanStack Query hooks.

### Code Formatting

Formatting is enforced via Lefthook pre-commit hooks:

- **Java**: Spotless with Palantir Java Format (AOSP style)
- **TypeScript/React**: Biome (single quotes, 4-space indent, 80-char lines)
- **Gradle scripts**: ktlint
- **SQL**: Prettier
- **Commits**: Conventional Commits via commitlint

### Environment Variables

| Variable | Description |
|---|---|
| `JWT_SECRET` | JWT signing key (min 32 characters) |
| `JWT_ACCESS_TOKEN_EXPIRY` | Access token TTL in seconds (default: 900) |
| `JWT_REFRESH_TOKEN_EXPIRY` | Refresh token TTL in seconds (default: 604800) |
| `STRIPE_SECRET_KEY` | Stripe secret API key |
| `STRIPE_WEBHOOK_SECRET` | Stripe webhook signing secret |
| `STRIPE_SUCCESS_URL` | Redirect URL after successful payment |
| `STRIPE_CANCEL_URL` | Redirect URL after cancelled payment |
| `CORS_ALLOWED_ORIGINS` | Allowed CORS origins |
| `VALKEY_HOST` | Valkey/Redis host (default: localhost) |
| `VALKEY_PORT` | Valkey/Redis port (default: 6379) |
| `BOOKING_MAX_SEATS` | Maximum seats per booking (default: 5) |

## Testing

- **Backend**: JUnit 5 + TestContainers (spins up PostgreSQL in Docker automatically)
- **Frontend**: Vitest + Testing Library + happy-dom

```bash
# Backend (requires Docker running)
cd backend && ./gradlew test

# Frontend
cd frontend/customer && bun run test
```

## Licence

GPL-3.0-only
