## Context

This is a greenfield multi-platform train ticket booking system requiring
initial project setup. The system consists of three main applications: Spring
Boot backend API, Kotlin Compose Multiplatform customer app
(Android/iOS/Desktop), and Next.js admin dashboard. All services will be
containerized and orchestrated via Docker Compose for local development.

Current state: Empty repository with only git initialization.

## Goals / Non-Goals

**Goals:**

- Establish monorepo structure with clear module boundaries
- Configure build systems for all three platforms (Gradle for JVM, Bun for
  Node.js)
- Setup PostgreSQL with Flyway migrations for database versioning
- Create Docker Compose environment for local development
- Define OpenAPI contract as single source of truth for API
- Enable immediate development start after initialization

**Non-Goals:**

- Implementing business logic or features (handled in future changes)
- Production deployment configuration (Kubernetes, cloud infrastructure)
- CI/CD pipeline setup (GitHub Actions will be added later)
- Authentication/authorization implementation (separate change)
- Payment gateway integration (separate change)

## Decisions

### 1. Monorepo Structure

**Decision:** Use monorepo with separate build systems per module

**Rationale:**

- Shared domain models and API contracts across all frontends
- Coordinated releases and versioning
- Simplified local development (single docker-compose up)
- Easier cross-module refactoring

**Alternative considered:** Multi-repo (rejected due to coordination overhead)

### 2. Database: PostgreSQL

**Decision:** PostgreSQL as primary database

**Rationale:**

- Strong ACID compliance required for booking transactions
- Excellent support for pessimistic locking (SELECT ... FOR UPDATE)
- Robust concurrent transaction handling
- JSON support for flexible data when needed

**Alternative considered:** MySQL (acceptable but weaker transaction handling),
MongoDB (rejected - lacks ACID guarantees)

### 3. Migration Tool: Flyway

**Decision:** Flyway for database migrations

**Rationale:**

- SQL-based migrations (simple, readable)
- Excellent Spring Boot integration
- Lower learning curve than Liquibase
- Version control friendly

**Alternative considered:** Liquibase (more features but XML verbosity)

### 4. Build Tools

**Decision:**

- Backend: Gradle 8+ with Kotlin DSL
- Customer App: Gradle with Kotlin Multiplatform plugin
- Admin: Bun (npm-compatible, faster than npm/yarn)

**Rationale:**

- Gradle: Industry standard for JVM projects, excellent Kotlin support
- Bun: 2-3x faster installs, built-in TypeScript support, Next.js compatible

### 5. API Contract: OpenAPI 3.0

**Decision:** OpenAPI specification in `shared/api-contracts/openapi.yaml`

**Rationale:**

- Single source of truth for API
- Can generate TypeScript types for admin frontend
- Can generate Kotlin data classes for customer app
- Documentation and contract testing

### 6. Environment Configuration

**Decision:**

- Backend: Spring profiles (application-{env}.yml)
- Customer: BuildConfig with compile-time constants
- Admin: .env files with NEXT*PUBLIC* prefix

**Rationale:**

- Platform-native approaches
- No secrets in version control
- Environment-specific overrides

## Risks / Trade-offs

### Risk: Kotlin Multiplatform iOS Build Complexity

**Impact:** Medium  
**Mitigation:**

- Start with Android and Desktop targets first
- iOS builds require macOS (document in README)
- Consider using Kotlin Multiplatform Mobile (KMM) wizard for initial setup

### Risk: Bun Production Maturity

**Impact:** Low  
**Mitigation:**

- Bun is stable for Next.js as of 2026
- Can fallback to Node.js if issues arise
- Monitor Bun release notes for breaking changes

### Risk: Monorepo Build Complexity

**Impact:** Medium  
**Mitigation:**

- Clear module boundaries prevent coupling
- Separate Dockerfiles per service
- Document build commands in root README

### Trade-off: Monorepo vs Multi-repo

**Chosen:** Monorepo  
**Trade-off:** Larger repository size, more complex CI/CD vs easier
coordination  
**Justification:** Early-stage project benefits from tight integration; can
split later if needed

### Trade-off: SQL Migrations vs ORM Auto-generation

**Chosen:** Explicit SQL migrations (Flyway)  
**Trade-off:** More manual work vs full control and transparency  
**Justification:** Database schema is critical for booking system; explicit
migrations prevent surprises
