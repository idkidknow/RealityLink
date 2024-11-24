rootProject.name = "reallink"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

includeBuild("mod-main")
includeBuild("core")

includeBuild("platform/forge118")
