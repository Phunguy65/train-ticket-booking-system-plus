package io.github.phunguy65.ttbs.backend.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Layer dependency rules for the backend's domain-oriented package structure.
 *
 * <p>Allowed dependency direction within each domain area:
 * <pre>
 *   web → application → domain
 *         infrastructure → domain
 *         infrastructure → application
 * </pre>
 *
 * <p>Spring stereotype annotations (@Service, @Transactional) are permitted in the
 * application layer. Only the domain layer must remain fully framework-free.
 */
class LayerRules {

    // ── Domain independence ────────────────────────────────────────────────

    /**
     * Domain classes must not depend on non-shared application layer classes.
     *
     * <p>{@code shared.application.*} types (e.g. {@code PageResponse}) are treated as shared
     * kernel contracts and are permitted in domain repository port signatures.
     */
    @ArchTest
    static final ArchRule domain_must_not_depend_on_application = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat(JavaClass.Predicates.resideInAPackage("..application..")
                    .and(JavaClass.Predicates.resideOutsideOfPackage("..shared.application..")))
            .because("Domain must not depend on application layer;"
                    + " shared.application types are permitted as shared kernel contracts");

    @ArchTest
    static final ArchRule domain_must_not_depend_on_infrastructure = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..infrastructure..")
            .because("Domain must not depend on infrastructure details");

    @ArchTest
    static final ArchRule domain_must_not_depend_on_spring = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "org.springframework.stereotype..",
                    "org.springframework.web..",
                    "org.springframework.data..",
                    "org.springframework.security..",
                    "jakarta.persistence..")
            .because("Domain must be framework-agnostic");

    // ── Application layer ─────────────────────────────────────────────────

    @ArchTest
    static final ArchRule application_must_not_depend_on_infrastructure = noClasses()
            .that()
            .resideInAPackage("..application..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..infrastructure..")
            .because("Application must not depend on infrastructure; use ports instead");

    @ArchTest
    static final ArchRule application_must_not_depend_on_web = noClasses()
            .that()
            .resideInAPackage("..application..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..infrastructure.web..")
            .because("Application must not depend on the web/presentation layer");

    // ── Infrastructure layer ──────────────────────────────────────────────

    @ArchTest
    static final ArchRule infrastructure_must_not_depend_on_web = noClasses()
            .that()
            .resideInAPackage("..infrastructure.persistence..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..infrastructure.web..")
            .because("Persistence adapters must not depend on web adapters");

    // ── Web / Controller layer ────────────────────────────────────────────

    @ArchTest
    static final ArchRule controllers_must_not_access_repositories = noClasses()
            .that()
            .resideInAPackage("..infrastructure.web..")
            .and()
            .haveSimpleNameEndingWith("Controller")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..domain.repository..", "..infrastructure.persistence..")
            .because("Controllers must go through use cases, not repositories directly");

    @ArchTest
    static final ArchRule controllers_must_not_access_jpa_entities = noClasses()
            .that()
            .resideInAPackage("..infrastructure.web..")
            .and()
            .haveSimpleNameEndingWith("Controller")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..infrastructure.persistence..")
            .because("Controllers must not reference JPA entities; use response DTOs");
}
