import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("org.jlleitschuh.gradle.ktlint")
}

group = "io.github.phunguy65.ttbs.conventions"
version = "0.0.1-SNAPSHOT"

val libs = the<LibrariesForLibs>()

dependencies {
    ktlintRuleset(libs.ktlint.compose.rules)
}

ktlint {
    version.set(libs.versions.ktlint.get())
    verbose.set(true)
    android.set(false)
    outputToConsole.set(true)
    ignoreFailures.set(false)
    enableExperimentalRules.set(true)
    filter {
        include("**/src/**/*.kt")
        include("**/*.gradle.kts")
        exclude("**/build/**")
        exclude("**/generated/**")
        exclude("**/gradle/**")
        exclude("**/bin/**")
        exclude("**/third-party/**")
    }
    additionalEditorconfig.set(
        mapOf(
            "indent_size" to "4",
            "indent_style" to "space",
            "insert_final_newline" to "true",
            "ktlint_standard_trailing-comma-on-call-site" to "disabled",
            "ktlint_standard_trailing-comma-on-declaration-site" to "disabled"
        )
    )
}
