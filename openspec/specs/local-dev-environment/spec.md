# Capability: local-dev-environment

## Purpose

Docker Compose orchestration for local development environment with all
services.

## Requirements

### Requirement: Docker Compose configuration

The system SHALL provide a docker-compose.yml file for orchestrating all
services locally.

#### Scenario: Docker Compose file exists

- **WHEN** project initialization completes
- **THEN** docker-compose.yml exists in the project root

#### Scenario: All services defined

- **WHEN** docker-compose.yml is examined
- **THEN** it defines services for postgres, backend, customer-app, and admin

### Requirement: PostgreSQL service

The system SHALL configure PostgreSQL database service with persistent volume.

#### Scenario: PostgreSQL service configured

- **WHEN** docker-compose.yml is examined
- **THEN** postgres service uses postgres:15-alpine image with environment
  variables for database name, user, and password

#### Scenario: Database persistence

- **WHEN** docker-compose.yml is examined
- **THEN** postgres service has a named volume for data persistence

### Requirement: Backend service

The system SHALL configure backend service with database dependency.

#### Scenario: Backend service configured

- **WHEN** docker-compose.yml is examined
- **THEN** backend service builds from ./backend/Dockerfile and depends_on
  postgres

#### Scenario: Backend port exposed

- **WHEN** docker-compose.yml is examined
- **THEN** backend service exposes port 8080

### Requirement: Admin service

The system SHALL configure admin dashboard service with backend dependency.

#### Scenario: Admin service configured

- **WHEN** docker-compose.yml is examined
- **THEN** admin service builds from ./frontend/admin/Dockerfile and depends_on
  backend

#### Scenario: Admin port exposed

- **WHEN** docker-compose.yml is examined
- **THEN** admin service exposes port 3000

### Requirement: Environment variables

The system SHALL provide environment variable configuration for service
communication.

#### Scenario: Backend database connection

- **WHEN** docker-compose.yml is examined
- **THEN** backend service has DATABASE_URL environment variable pointing to
  postgres service

#### Scenario: Admin API connection

- **WHEN** docker-compose.yml is examined
- **THEN** admin service has NEXT_PUBLIC_API_URL environment variable pointing
  to backend service

### Requirement: Network configuration

The system SHALL create a shared network for service communication.

#### Scenario: Network defined

- **WHEN** docker-compose.yml is examined
- **THEN** it defines a custom bridge network for all services

### Requirement: One-command startup

The system SHALL enable starting all services with a single command.

#### Scenario: Services start successfully

- **WHEN** developer runs `docker-compose up`
- **THEN** all services (postgres, backend, admin) start and are accessible
