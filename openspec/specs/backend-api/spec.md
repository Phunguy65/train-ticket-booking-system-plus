# Capability: backend-api

## Purpose

Spring Boot backend API with layered architecture for the train ticket booking
system.

## Requirements

### Requirement: Backend project structure

The system SHALL create a Spring Boot backend project with layered architecture
following industry best practices.

#### Scenario: Project structure created

- **WHEN** project initialization completes
- **THEN** backend directory contains src/main/java with controller, service,
  repository, model, dto, security, and config packages

### Requirement: Gradle build configuration

The system SHALL configure Gradle 8+ with Kotlin DSL for building the Spring
Boot application.

#### Scenario: Gradle build file exists

- **WHEN** project initialization completes
- **THEN** backend/build.gradle.kts exists with Spring Boot plugin and
  dependencies

#### Scenario: Build executes successfully

- **WHEN** developer runs `./gradlew build`
- **THEN** project compiles without errors

### Requirement: Spring Boot dependencies

The system SHALL include essential Spring Boot dependencies for web, data, and
security.

#### Scenario: Core dependencies configured

- **WHEN** build.gradle.kts is examined
- **THEN** it includes spring-boot-starter-web, spring-boot-starter-data-jpa,
  spring-boot-starter-security, and spring-boot-starter-validation

### Requirement: Application configuration

The system SHALL provide environment-specific configuration files using Spring
profiles.

#### Scenario: Configuration files exist

- **WHEN** project initialization completes
- **THEN** src/main/resources contains application.yml, application-dev.yml,
  application-staging.yml, and application-prod.yml

#### Scenario: Database configuration present

- **WHEN** application-dev.yml is examined
- **THEN** it contains PostgreSQL datasource configuration

### Requirement: Main application class

The system SHALL create a Spring Boot main application class as the entry point.

#### Scenario: Application class exists

- **WHEN** project initialization completes
- **THEN** a class annotated with @SpringBootApplication exists in the base
  package

### Requirement: Docker support

The system SHALL provide a Dockerfile for containerizing the backend
application.

#### Scenario: Dockerfile exists

- **WHEN** project initialization completes
- **THEN** backend/Dockerfile exists with multi-stage build configuration

#### Scenario: Docker image builds successfully

- **WHEN** developer runs `docker build -t backend .`
- **THEN** Docker image is created without errors
