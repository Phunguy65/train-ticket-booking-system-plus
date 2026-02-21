import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.spotlessConvention)
}

kotlin {
    dependencies {
        implementation(projects.composeApp)
        implementation(compose.desktop.currentOs)
        implementation(libs.kotlinx.coroutinesSwing)
    }
}

compose.desktop {
    application {
        mainClass = "io.github.phunguy65.ttbs.customer.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "ttbs-customer-desktop"
            packageVersion = "1.0.0"
        }
    }
}
