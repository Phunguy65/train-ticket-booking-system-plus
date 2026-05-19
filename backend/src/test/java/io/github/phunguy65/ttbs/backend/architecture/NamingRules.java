package io.github.phunguy65.ttbs.backend.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Naming and package-location rules derived from the backend's current package conventions:
 *
 * <ul>
 *   <li>{@code *UseCase}           → {@code ..application.usecase..}
 *   <li>{@code *Controller}        → {@code ..infrastructure.web..}
 *   <li>{@code *Entity} (JPA)      → {@code ..infrastructure.persistence..}, never in domain
 *   <li>{@code *Repository} iface  → {@code ..domain.repository..}
 *   <li>{@code *Port} iface        → {@code ..application.port..}
 *   <li>{@code *RepositoryAdapter} → {@code ..infrastructure.persistence..}
 * </ul>
 */
class NamingRules {

    // ── UseCase ───────────────────────────────────────────────────────────

    @ArchTest
    static final ArchRule use_cases_must_reside_in_application_usecase = classes()
            .that()
            .haveSimpleNameEndingWith("UseCase")
            .should()
            .resideInAPackage("..application.usecase..")
            .because("UseCase classes belong in the application.usecase package");

    // ── Controller ────────────────────────────────────────────────────────

    @ArchTest
    static final ArchRule controllers_must_reside_in_infrastructure_web = classes()
            .that()
            .haveSimpleNameEndingWith("Controller")
            .should()
            .resideInAPackage("..infrastructure.web..")
            .because("Controllers belong in the infrastructure.web package");

    // ── JPA Entity ────────────────────────────────────────────────────────

    @ArchTest
    static final ArchRule jpa_entities_must_reside_in_infrastructure_persistence = classes()
            .that()
            .areAnnotatedWith("jakarta.persistence.Entity")
            .should()
            .resideInAPackage("..infrastructure.persistence..")
            .because("JPA entities are infrastructure details and must not leak into domain");

    @ArchTest
    static final ArchRule entity_suffix_classes_must_not_reside_in_domain = noClasses()
            .that()
            .haveSimpleNameEndingWith("Entity")
            .should()
            .resideInAPackage("..domain..")
            .because("Classes named *Entity are JPA adapters and must live in"
                    + " infrastructure.persistence, not in the domain layer");

    @ArchTest
    static final ArchRule entity_suffix_classes_must_reside_in_infrastructure_persistence =
            classes()
                    .that()
                    .haveSimpleNameEndingWith("Entity")
                    .should()
                    .resideInAPackage("..infrastructure.persistence..")
                    .because(
                            "*Entity classes are JPA adapters and belong in infrastructure.persistence");

    // ── Repository port (domain interface) ───────────────────────────────

    @ArchTest
    static final ArchRule repository_interfaces_must_reside_in_domain_repository = classes()
            .that()
            .haveSimpleNameEndingWith("Repository")
            .and()
            .areInterfaces()
            .and()
            .doNotHaveSimpleName("JpaRepository")
            .and()
            .haveSimpleNameNotEndingWith("JpaRepository")
            .should()
            .resideInAPackage("..domain.repository..")
            .because("Repository port interfaces are domain contracts");

    // ── Infrastructure port (application interface) ───────────────────────

    @ArchTest
    static final ArchRule port_interfaces_must_reside_in_application_port = classes()
            .that()
            .haveSimpleNameEndingWith("Port")
            .and()
            .areInterfaces()
            .should()
            .resideInAPackage("..application.port..")
            .because("Port interfaces define the application's outbound contracts");

    // ── Repository adapter (infrastructure implementation) ────────────────

    @ArchTest
    static final ArchRule repository_adapters_must_reside_in_infrastructure_persistence = classes()
            .that()
            .haveSimpleNameEndingWith("RepositoryAdapter")
            .should()
            .resideInAPackage("..infrastructure.persistence..")
            .because("RepositoryAdapter implementations are infrastructure details");
}
