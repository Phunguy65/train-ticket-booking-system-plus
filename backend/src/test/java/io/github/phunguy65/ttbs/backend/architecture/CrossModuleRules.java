package io.github.phunguy65.ttbs.backend.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Cross-module isolation rules (minimal strategy).
 *
 * <p>Spring Modulith already enforces module boundaries at runtime. These rules add a
 * compile-time safety net by preventing direct access to another module's
 * {@code infrastructure} internals. Modules may only consume what is explicitly
 * exported via {@code @NamedInterface} (i.e. {@code domain.model}, {@code domain.event},
 * {@code application.port}).
 *
 * <p>Modules: booking, payment, train, station, user (shared is open to all).
 */
class CrossModuleRules {

    private static final String[] MODULES = {"booking", "payment", "train", "station", "user"};

    // ── booking ───────────────────────────────────────────────────────────

    @ArchTest
    static final ArchRule booking_infrastructure_must_not_access_other_module_infrastructure =
            noClasses()
                    .that()
                    .resideInAPackage("..booking.infrastructure..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "..payment.infrastructure..",
                            "..train.infrastructure..",
                            "..station.infrastructure..",
                            "..user.infrastructure..")
                    .because(
                            "booking.infrastructure must not reach into other modules' infrastructure");

    // ── payment ───────────────────────────────────────────────────────────

    @ArchTest
    static final ArchRule payment_infrastructure_must_not_access_other_module_infrastructure =
            noClasses()
                    .that()
                    .resideInAPackage("..payment.infrastructure..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "..booking.infrastructure..",
                            "..train.infrastructure..",
                            "..station.infrastructure..",
                            "..user.infrastructure..")
                    .because(
                            "payment.infrastructure must not reach into other modules' infrastructure");

    // ── train ─────────────────────────────────────────────────────────────

    @ArchTest
    static final ArchRule train_infrastructure_must_not_access_other_module_infrastructure =
            noClasses()
                    .that()
                    .resideInAPackage("..train.infrastructure..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "..booking.infrastructure..",
                            "..payment.infrastructure..",
                            "..station.infrastructure..",
                            "..user.infrastructure..")
                    .because(
                            "train.infrastructure must not reach into other modules' infrastructure");

    // ── station ───────────────────────────────────────────────────────────

    @ArchTest
    static final ArchRule station_infrastructure_must_not_access_other_module_infrastructure =
            noClasses()
                    .that()
                    .resideInAPackage("..station.infrastructure..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "..booking.infrastructure..",
                            "..payment.infrastructure..",
                            "..train.infrastructure..",
                            "..user.infrastructure..")
                    .because(
                            "station.infrastructure must not reach into other modules' infrastructure");

    // ── user ──────────────────────────────────────────────────────────────

    @ArchTest
    static final ArchRule user_infrastructure_must_not_access_other_module_infrastructure =
            noClasses()
                    .that()
                    .resideInAPackage("..user.infrastructure..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "..booking.infrastructure..",
                            "..payment.infrastructure..",
                            "..train.infrastructure..",
                            "..station.infrastructure..")
                    .because(
                            "user.infrastructure must not reach into other modules' infrastructure");
}
