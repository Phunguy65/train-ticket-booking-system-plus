# ADDED Requirements

## Requirement: Database migration directory

The system SHALL create a directory structure for Flyway database migrations.

#### Scenario: Migration directory exists

- **WHEN** project initialization completes
- **THEN** database/migrations directory exists

## Requirement: Initial schema migration

The system SHALL provide an initial migration file for core database tables.

#### Scenario: Initial migration file exists

- **WHEN** project initialization completes
- **THEN** database/migrations/V1\_\_initial_schema.sql exists

#### Scenario: Core tables defined

- **WHEN** V1\_\_initial_schema.sql is examined
- **THEN** it contains CREATE TABLE statements for users, trains, routes,
  stations, seats, and bookings

## Requirement: Database indexes

The system SHALL define indexes for performance-critical queries.

#### Scenario: Indexes created

- **WHEN** V1\_\_initial_schema.sql is examined
- **THEN** it contains CREATE INDEX statements for seats(train_id, status),
  bookings(user_id), and bookings(train_id)

## Requirement: Foreign key constraints

The system SHALL enforce referential integrity with foreign key constraints.

#### Scenario: Foreign keys defined

- **WHEN** V1\_\_initial_schema.sql is examined
- **THEN** bookings table has foreign keys to users, trains, and seats tables

## Requirement: Seed data directory

The system SHALL provide directories for environment-specific seed data.

#### Scenario: Seed directories exist

- **WHEN** project initialization completes
- **THEN** database/seeds/dev and database/seeds/staging directories exist

## Requirement: Schema documentation

The system SHALL provide documentation for the database schema.

#### Scenario: ERD documentation exists

- **WHEN** project initialization completes
- **THEN** database/schema/erd.md exists with entity relationship descriptions

## Requirement: Flyway configuration

The system SHALL configure Flyway in Spring Boot application properties.

#### Scenario: Flyway enabled in config

- **WHEN** backend application.yml is examined
- **THEN** spring.flyway.enabled is set to true and locations point to
  classpath:db/migration
