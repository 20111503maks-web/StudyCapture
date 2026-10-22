import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // java-library provides the `api` configuration: our public API exposes core:model types.
    `java-library`
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

dependencies {
    api(project(":core:model"))
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit)
}
