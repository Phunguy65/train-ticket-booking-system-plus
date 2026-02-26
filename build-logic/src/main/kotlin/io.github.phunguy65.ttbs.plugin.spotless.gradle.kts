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
        targetExclude("**/build/**", "**/generated/**", "**/third-party/**")
        palantirJavaFormat(libs.versions.palantirJavaFormat.get()).style("AOSP")
        formatAnnotations()
        trimTrailingWhitespace()
        endWithNewline()
        importOrder()
        removeUnusedImports()
    }

    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**", "**/generated/**", "**/gradle/**", "**/bin/**", "**/third-party/**")
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(
            mapOf(
                "indent_size" to "4",
                "indent_style" to "space",
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
    }

    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**", "**/generated/**", "**/gradle/**", "**/bin/**", "**/third-party/**")
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(
            mapOf(
                "indent_size" to "4",
                "indent_style" to "space",
                "insert_final_newline" to "true",
                "ktlint_standard_trailing-comma-on-call-site" to "disabled",
                "ktlint_standard_trailing-comma-on-declaration-site" to "disabled"
            )
        )
        trimTrailingWhitespace()
        endWithNewline()
    }
}
