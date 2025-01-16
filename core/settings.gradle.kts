@file:Suppress("UnstableApiUsage")

rootProject.name = "reallink-core"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.8.0")
}

includeBuild("../core-api") {
    dependencySubstitution {
        substitute(module("com.idkidknow.mcreallink:reallink-core-api_3")).using(project(":"))
    }
}
