plugins {
    `kotlin-dsl`
    alias(libs.plugins.spotlessConvention)
    alias(libs.plugins.ktlintConvention)
}

group = "io.github.phunguy65.ttbs"
// x-release-please-start-version
version = "0.1.0"
// x-release-please-end

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
