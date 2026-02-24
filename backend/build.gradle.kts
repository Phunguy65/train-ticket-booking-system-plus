plugins {
    java
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDependencyManagement)
    alias(libs.plugins.hibernateOrm)
    alias(libs.plugins.graalvmNative)
    alias(libs.plugins.spotlessConvention)
}

group = "io.github.phunguy65.ttbs"
version = "0.0.1-SNAPSHOT"
description = "backend"

java {
    toolchain {
        languageVersion =
            JavaLanguageVersion.of(
                libs.versions.java
                    .get()
                    .toInt()
            )
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

dependencies {
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.amqp)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.integration)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.rabbit.stream)
    implementation(libs.spring.integration.amqp)
    implementation(libs.spring.integration.http)
    implementation(libs.spring.integration.jpa)
    implementation(libs.spring.modulith.events.api)
    implementation(libs.spring.modulith.starter.core)
    implementation(libs.spring.modulith.starter.jpa)
    implementation(libs.spring.security.messaging)
    implementation(libs.jjwt.api)
    implementation(libs.jackson.databind.nullable)
    compileOnly(libs.jspecify)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)
    developmentOnly(libs.spring.boot.devtools)
    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.spring.modulith.actuator)
    runtimeOnly(libs.spring.modulith.events.amqp)
    runtimeOnly(libs.spring.modulith.observability)
    annotationProcessor(libs.spring.boot.configuration.processor)
    testImplementation(libs.spring.boot.starter.actuator.test)
    testImplementation(libs.spring.boot.starter.data.jpa.test)
    testImplementation(libs.spring.boot.starter.security.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.integration.test)
    testImplementation(libs.spring.modulith.starter.test)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.h2)
    testImplementation(libs.archunit.junit5)
}

dependencyManagement {
    imports {
        mavenBom(
            libs.spring.modulith.bom
                .get()
                .toString()
        )
    }
}

hibernate {
    enhancement {
        enableAssociationManagement = true
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Disable AOT test processing to allow regular JVM test execution.
// Native image test compilation (nativeTest) can still be run explicitly.
tasks.named("processTestAot") {
    enabled = false
}
