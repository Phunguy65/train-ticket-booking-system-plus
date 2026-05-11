plugins {
    `kotlin-dsl`
}

group = "io.github.phunguy65.ttbs"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencies {
    implementation(plugin(libs.plugins.spotless))
    implementation(plugin(libs.plugins.ktlint))
    implementation(plugin(libs.plugins.composeCompiler))
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}

fun DependencyHandlerScope.plugin(plugin: Provider<PluginDependency>) = plugin.map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" }
