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
    implementation(libs.spring.boot.starter.flyway)
    runtimeOnly(libs.flyway.database.postgresql)
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
    implementation(libs.spring.security.messaging)
    implementation(libs.jjwt.api)
    implementation(libs.jackson.databind.nullable)
    compileOnly(libs.jspecify)
    implementation(libs.stripe.java)
    implementation(libs.uuid.creator)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)
    developmentOnly(libs.spring.boot.devtools)
    runtimeOnly(libs.postgresql)
    annotationProcessor(libs.spring.boot.configuration.processor)
    testImplementation(libs.spring.boot.starter.actuator.test)
    testImplementation(libs.spring.boot.starter.data.jpa.test)
    testImplementation(libs.spring.boot.starter.security.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.integration.test)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(platform(libs.testcontainers.bom))
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.rabbitmq)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.archunit.junit5)
}

hibernate {
    enhancement {
        enableAssociationManagement = false
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
}
