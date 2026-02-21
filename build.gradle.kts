plugins {
    `kotlin-dsl`
    alias(libs.plugins.spotlessConvention)
}

group = "io.github.phunguy65.ttbs"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
