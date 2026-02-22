import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("com.diffplug.spotless")
}

group = "io.github.phunguy65.ttbs.conventions"
version = "0.0.1-SNAPSHOT"

val libs = the<LibrariesForLibs>()

spotless {
    java {
        target("**/src/**/*.java")
        targetExclude("**/build/**", "**/generated/**")
        palantirJavaFormat(libs.versions.palantirJavaFormat.get()).style("AOSP")
        formatAnnotations()
        trimTrailingWhitespace()
        endWithNewline()
        importOrder()
        removeUnusedImports()
        licenseHeader("")
    }

    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**", "**/generated/**", "**/gradle/**")
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(
            mapOf(
                "indent_size" to "4",
                "continuation_indent_size" to "4",
                "insert_final_newline" to "true",
                "ktlint_standard_trailing-comma-on-call-site" to "disabled",
                "ktlint_standard_trailing-comma-on-declaration-site" to "disabled"
            )
        ).customRuleSets(
            listOf(
                libs.ktlint.compose.rules.get().toString()
            )
        )
        trimTrailingWhitespace()
        endWithNewline()
        licenseHeader("")
    }

    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**", "**/generated/**")
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(
            mapOf(
                "indent_size" to "4",
                "continuation_indent_size" to "4",
                "insert_final_newline" to "true",
                "ktlint_standard_trailing-comma-on-call-site" to "disabled",
                "ktlint_standard_trailing-comma-on-declaration-site" to "disabled"
            )
        )
        trimTrailingWhitespace()
        endWithNewline()
    }
}
