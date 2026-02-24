package io.github.phunguy65.ttbs.backend.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

class ArchitectureTest {

    private static final String BASE_PACKAGE = "io.github.phunguy65.ttbs.backend";
    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);
    }

    @Test
    void jpaEntitiesShouldNotResideInDomainPackages() {
        ArchRule rule = noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .beAnnotatedWith(Entity.class)
                .because("JPA entities must not live in domain packages");
        rule.check(importedClasses);
    }

    @Test
    void domainClassesShouldNotDependOnSpringOrJpa() {
        ArchRule rule = noClasses()
                .that()
                .resideInAPackage("..domain..")
                .and()
                .resideOutsideOfPackage("..shared.domain..")
                .and()
                .haveSimpleNameNotEndingWith("package-info")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "jakarta.transaction..",
                        "tools.jackson..",
                        "com.fasterxml.jackson..")
                .because("Domain classes must be pure Java with no framework dependencies");
        rule.check(importedClasses);
    }

    @Test
    void applicationClassesShouldOnlyDependOnDomainAndShared() {
        ArchRule rule = noClasses()
                .that()
                .resideInAPackage("..application..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..infrastructure..")
                .because("Application layer must not depend on infrastructure layer");
        rule.check(importedClasses);
    }

    @Test
    void useCasesShouldBeAnnotatedWithTransactional() {
        ArchRule rule = methods()
                .that()
                .areDeclaredInClassesThat()
                .haveSimpleNameEndingWith("UseCase")
                .and()
                .haveNameMatching("execute")
                .should()
                .beAnnotatedWith(Transactional.class)
                .because("All use case execute() methods must be @Transactional");
        rule.check(importedClasses);
    }

    @Test
    void jpaEntitiesShouldResideInPersistencePackages() {
        ArchRule rule = classes()
                .that()
                .areAnnotatedWith(Entity.class)
                .should()
                .resideInAPackage("..infrastructure.persistence..")
                .because(
                        "JPA entities must reside exclusively in infrastructure/persistence packages");
        rule.check(importedClasses);
    }
}
