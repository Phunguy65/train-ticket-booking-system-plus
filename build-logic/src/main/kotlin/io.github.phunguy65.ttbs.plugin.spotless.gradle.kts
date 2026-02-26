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
}
