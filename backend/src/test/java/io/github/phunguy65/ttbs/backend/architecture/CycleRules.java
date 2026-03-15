package io.github.phunguy65.ttbs.backend.architecture;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Cycle detection rules.
 *
 * <p>Ensures the bounded-context modules (booking, payment, train, station, user) form an
 * acyclic dependency graph. The {@code shared} module is intentionally excluded because it
 * is an open kernel depended on by all modules — its presence in the slice graph would
 * produce false-positive cycles.
 */
class CycleRules {

    /**
     * Matches slices for each bounded context, explicitly excluding {@code shared}.
     * Pattern captures the first segment after the base package, so each module becomes
     * one slice: booking, payment, train, station, user.
     */
    @ArchTest
    static final ArchRule no_cycles_between_bounded_context_modules = slices().matching(
                    "io.github.phunguy65.ttbs.backend.(booking|payment|train|station|user)..")
            .should()
            .beFreeOfCycles()
            .because("Bounded-context modules must form an acyclic dependency graph");
}
