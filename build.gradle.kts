plugins {
    `kotlin-dsl`
    alias(libs.plugins.spotlessConvention)
    alias(libs.plugins.ktlintConvention)
}

group = "io.github.phunguy65.ttbs"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}

ktlint {
    filter {
        setIncludes(emptyList<String>())
        setExcludes(emptyList<String>())

        exclude("**/generated/**")
        exclude("**/build/**")
        exclude("**/gradle/**")
        exclude("**/bin/**")

        include("**/build-logic/**/*.kt")
        include("**/build-logic/**/*.gradle.kts")
        include("*.gradle.kts")
    }
}
