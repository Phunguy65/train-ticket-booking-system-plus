package io.github.phunguy65.ttbs.backend.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Architecture test suite for the train-ticket-booking-system backend.
 *
 * <p>The backend is organized into domain-oriented packages at the top level. These tests keep
 * the internal package structure consistent by enforcing layer dependency direction and
 * naming/location conventions. Rules are grouped into focused classes and imported here via
 * {@code @ArchTest} field references so that {@link AnalyzeClasses} caching is shared across all
 * of them.
 *
 * <ul>
 *   <li>{@link LayerRules}  - layer dependency direction (domain / application / infrastructure / web)
 *   <li>{@link NamingRules} - naming-convention / location contracts
 * </ul>
 */
@AnalyzeClasses(
        packages = "io.github.phunguy65.ttbs.backend",
        importOptions = {ImportOption.DoNotIncludeTests.class, ImportOption.DoNotIncludeJars.class})
public class ArchitectureTest {

    // ── Layer rules ───────────────────────────────────────────────────────

    @ArchTest
    static final ArchRule domain_must_not_depend_on_application =
            LayerRules.domain_must_not_depend_on_application;

    @ArchTest
    static final ArchRule domain_must_not_depend_on_infrastructure =
            LayerRules.domain_must_not_depend_on_infrastructure;

    @ArchTest
    static final ArchRule domain_must_not_depend_on_spring =
            LayerRules.domain_must_not_depend_on_spring;

    @ArchTest
    static final ArchRule application_must_not_depend_on_infrastructure =
            LayerRules.application_must_not_depend_on_infrastructure;

    @ArchTest
    static final ArchRule application_must_not_depend_on_web =
            LayerRules.application_must_not_depend_on_web;

    @ArchTest
    static final ArchRule infrastructure_must_not_depend_on_web =
            LayerRules.infrastructure_must_not_depend_on_web;

    @ArchTest
    static final ArchRule controllers_must_not_access_repositories =
            LayerRules.controllers_must_not_access_repositories;

    @ArchTest
    static final ArchRule controllers_must_not_access_jpa_entities =
            LayerRules.controllers_must_not_access_jpa_entities;

    // ── Naming rules ──────────────────────────────────────────────────────

    @ArchTest
    static final ArchRule use_cases_must_reside_in_application_usecase =
            NamingRules.use_cases_must_reside_in_application_usecase;

    @ArchTest
    static final ArchRule controllers_must_reside_in_infrastructure_web =
            NamingRules.controllers_must_reside_in_infrastructure_web;

    @ArchTest
    static final ArchRule jpa_entities_must_reside_in_infrastructure_persistence =
            NamingRules.jpa_entities_must_reside_in_infrastructure_persistence;

    @ArchTest
    static final ArchRule entity_suffix_classes_must_not_reside_in_domain =
            NamingRules.entity_suffix_classes_must_not_reside_in_domain;

    @ArchTest
    static final ArchRule entity_suffix_classes_must_reside_in_infrastructure_persistence =
            NamingRules.entity_suffix_classes_must_reside_in_infrastructure_persistence;

    @ArchTest
    static final ArchRule repository_interfaces_must_reside_in_domain_repository =
            NamingRules.repository_interfaces_must_reside_in_domain_repository;

    @ArchTest
    static final ArchRule port_interfaces_must_reside_in_application_port =
            NamingRules.port_interfaces_must_reside_in_application_port;

    @ArchTest
    static final ArchRule repository_adapters_must_reside_in_infrastructure_persistence =
            NamingRules.repository_adapters_must_reside_in_infrastructure_persistence;
}
