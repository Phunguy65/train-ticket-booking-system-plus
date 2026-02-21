# Why

This is a greenfield project requiring initial setup of a multi-platform train
ticket booking system. Without proper project structure, build configurations,
and foundational architecture, development cannot proceed efficiently. This
change establishes the complete project foundation including backend (Spring
Boot), customer frontend (Kotlin Compose Multiplatform), admin frontend
(Next.js + Bun), and database infrastructure.

## What Changes

- Initialize monorepo structure with clear module boundaries
- Setup Spring Boot backend with Gradle build configuration
- Setup Kotlin Compose Multiplatform customer app with multi-platform targets
  (Android, iOS, Desktop)
- Setup Next.js + Bun admin dashboard with TypeScript configuration
- Configure PostgreSQL database with Flyway migration system
- Create Docker Compose for local development environment
- Setup shared API contracts directory with OpenAPI specification
- Configure environment-specific settings (dev, staging, prod)
- Initialize Git repository with appropriate .gitignore files

## Capabilities

### New Capabilities

- `backend-api`: Spring Boot REST API with layered architecture
  (controller/service/repository)
- `customer-app`: Kotlin Compose Multiplatform application for ticket booking
- `admin-dashboard`: Next.js web application for system management
- `database-schema`: PostgreSQL database with core tables and migration system
- `local-dev-environment`: Docker Compose setup for local development
- `api-contracts`: OpenAPI specification for REST API documentation

### Modified Capabilities

<!-- No existing capabilities to modify - this is initial project setup -->

## Impact

- Creates entire project structure from scratch
- Establishes build systems: Gradle (backend/customer), npm/Bun (admin)
- Introduces PostgreSQL as primary database
- Sets up Docker containerization for all services
- Defines API contract that all frontends will consume
- Establishes development workflow and environment configuration patterns
