# ADDED Requirements

## Requirement: Kotlin Multiplatform project structure

The system SHALL create a Kotlin Compose Multiplatform project with support for
Android, iOS, and Desktop targets.

#### Scenario: Project structure created

- **WHEN** project initialization completes
- **THEN** frontend/customer directory contains composeApp with src/commonMain,
  src/androidMain, src/iosMain, and src/desktopMain

## Requirement: Gradle multiplatform configuration

The system SHALL configure Gradle with Kotlin Multiplatform plugin and Compose
plugin.

#### Scenario: Build configuration exists

- **WHEN** project initialization completes
- **THEN** frontend/customer/build.gradle.kts exists with
  kotlin("multiplatform") and compose plugins

#### Scenario: Platform targets configured

- **WHEN** build.gradle.kts is examined
- **THEN** it declares androidTarget, jvm("desktop"), and iOS targets (iosX64,
  iosArm64, iosSimulatorArm64)

## Requirement: Shared business logic structure

The system SHALL organize shared code in commonMain with platform-specific
implementations.

#### Scenario: Common source structure

- **WHEN** project initialization completes
- **THEN** src/commonMain/kotlin contains ui, data, domain, and di packages

## Requirement: Compose dependencies

The system SHALL include Compose Multiplatform dependencies for UI development.

#### Scenario: Compose libraries configured

- **WHEN** build.gradle.kts is examined
- **THEN** commonMain dependencies include compose.runtime, compose.foundation,
  and compose.material3

## Requirement: Network client configuration

The system SHALL configure Ktor client for multiplatform HTTP networking.

#### Scenario: Ktor client dependency

- **WHEN** build.gradle.kts is examined
- **THEN** commonMain dependencies include ktor-client-core

#### Scenario: Platform-specific engines

- **WHEN** build.gradle.kts is examined
- **THEN** androidMain uses ktor-client-okhttp and iosMain uses
  ktor-client-darwin

## Requirement: Gradle properties

The system SHALL provide gradle.properties with Kotlin and Compose versions.

#### Scenario: Properties file exists

- **WHEN** project initialization completes
- **THEN** frontend/customer/gradle.properties exists with kotlin.version and
  compose.version properties
