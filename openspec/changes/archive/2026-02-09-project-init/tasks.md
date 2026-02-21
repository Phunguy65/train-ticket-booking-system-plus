## 1. Project Structure Setup

- [x] 1.1 Create root directory structure (backend/, frontend/, database/,
      shared/)
- [x] 1.2 Create backend module directories (src/main/java, src/main/resources,
      src/test)
- [x] 1.3 Create backend package structure (controller, service, repository,
      model, dto, security, config)
- [x] 1.4 Create frontend/customer directory structure (composeApp/src with
      commonMain, androidMain, iosMain, desktopMain)
- [x] 1.5 Create frontend/admin directory structure (src/app, src/components,
      src/lib, src/types)
- [x] 1.6 Create database directories (migrations/, seeds/dev, seeds/staging,
      schema/)
- [x] 1.7 Create shared/api-contracts directory

## 2. Backend Configuration

- [x] 2.1 Create backend/build.gradle.kts with Spring Boot plugin and
      dependencies
- [x] 2.2 Create backend/settings.gradle.kts
- [x] 2.3 Create backend/gradle.properties
- [x] 2.4 Create backend/src/main/resources/application.yml with base
      configuration
- [x] 2.5 Create backend/src/main/resources/application-dev.yml with PostgreSQL
      config
- [x] 2.6 Create backend/src/main/resources/application-staging.yml
- [x] 2.7 Create backend/src/main/resources/application-prod.yml
- [x] 2.8 Create Spring Boot main application class with @SpringBootApplication
- [x] 2.9 Create backend/Dockerfile with multi-stage build
- [x] 2.10 Create backend/.gitignore

## 3. Customer App Configuration

- [x] 3.1 Create frontend/customer/build.gradle.kts with Kotlin Multiplatform
      and Compose plugins
- [x] 3.2 Configure platform targets (androidTarget, jvm desktop, iOS targets)
- [x] 3.3 Add Compose dependencies to commonMain
- [x] 3.4 Add Ktor client dependencies (core in commonMain, okhttp in
      androidMain, darwin in iosMain)
- [x] 3.5 Create frontend/customer/gradle.properties with Kotlin and Compose
      versions
- [x] 3.6 Create frontend/customer/settings.gradle.kts
- [x] 3.7 Create commonMain package structure (ui, data, domain, di)
- [x] 3.8 Create frontend/customer/.gitignore

## 4. Admin Dashboard Configuration

- [x] 4.1 Create frontend/admin/package.json with Next.js, React, and TypeScript
      dependencies
- [x] 4.2 Create frontend/admin/next.config.js
- [x] 4.3 Create frontend/admin/tsconfig.json with strict mode
- [x] 4.4 Create frontend/admin/.env.example with NEXT_PUBLIC_API_URL
- [x] 4.5 Create frontend/admin/src/app/layout.tsx
- [x] 4.6 Create frontend/admin/src/app/page.tsx
- [x] 4.7 Create component directories (src/components/ui,
      src/components/features)
- [x] 4.8 Create frontend/admin/Dockerfile with Bun multi-stage build
- [x] 4.9 Create frontend/admin/.gitignore
- [x] 4.10 Run bun install to generate bun.lockb

## 5. Database Schema

- [x] 5.1 Create database/migrations/V1\_\_initial_schema.sql
- [x] 5.2 Add CREATE TABLE statement for users table
- [x] 5.3 Add CREATE TABLE statement for stations table
- [x] 5.4 Add CREATE TABLE statement for trains table
- [x] 5.5 Add CREATE TABLE statement for routes table
- [x] 5.6 Add CREATE TABLE statement for seats table
- [x] 5.7 Add CREATE TABLE statement for bookings table
- [x] 5.8 Add foreign key constraints to bookings table
- [x] 5.9 Add indexes for seats(train_id, status), bookings(user_id),
      bookings(train_id)
- [x] 5.10 Create database/schema/erd.md with entity relationship documentation
- [x] 5.11 Configure Flyway in backend application.yml

## 6. API Contracts

- [x] 6.1 Create shared/api-contracts/openapi.yaml
- [x] 6.2 Add OpenAPI 3.0 metadata (info, version, description)
- [x] 6.3 Define authentication endpoints (POST /api/v1/auth/login, POST
      /api/v1/auth/register)
- [x] 6.4 Define train endpoints (GET /api/v1/trains, GET /api/v1/trains/{id})
- [x] 6.5 Define booking endpoints (POST /api/v1/bookings, GET /api/v1/bookings,
      GET /api/v1/bookings/{id})
- [x] 6.6 Define schemas for User, Train, Booking, Seat, Error in
      components.schemas
- [x] 6.7 Define bearerAuth security scheme in components.securitySchemes
- [x] 6.8 Add security requirements to protected endpoints
- [x] 6.9 Document error responses (400, 401, 404, 500) for all endpoints

## 7. Docker Compose Setup

- [x] 7.1 Create docker-compose.yml in project root
- [x] 7.2 Define postgres service with postgres:15-alpine image
- [x] 7.3 Configure postgres environment variables (POSTGRES_DB, POSTGRES_USER,
      POSTGRES_PASSWORD)
- [x] 7.4 Add named volume for postgres data persistence
- [x] 7.5 Define backend service with build context ./backend
- [x] 7.6 Configure backend depends_on postgres
- [x] 7.7 Expose backend port 8080
- [x] 7.8 Add DATABASE_URL environment variable to backend service
- [x] 7.9 Define admin service with build context ./frontend/admin
- [x] 7.10 Configure admin depends_on backend
- [x] 7.11 Expose admin port 3000
- [x] 7.12 Add NEXT_PUBLIC_API_URL environment variable to admin service
- [x] 7.13 Define custom bridge network for all services

## 8. Root Configuration

- [x] 8.1 Create root .gitignore with common patterns
- [x] 8.2 Create README.md with project overview and setup instructions
- [x] 8.3 Verify all directories and files are created
- [x] 8.4 Test docker-compose up starts all services successfully
