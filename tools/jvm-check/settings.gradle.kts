// Standalone build for the pure-Kotlin modules only.
// Lets us compile and unit-test core logic on machines without the Android SDK:
//   cd tools/jvm-check && ../../gradlew test
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        // Fallback mirror when Maven Central rate-limits this machine.
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") { from(files("../../gradle/libs.versions.toml")) }
    }
}

rootProject.name = "jvm-check"

include(":core")
project(":core").projectDir = file("../../core")

listOf("model", "srs", "understanding").forEach { name ->
    include(":core:$name")
    project(":core:$name").projectDir = file("../../core/$name")
}
