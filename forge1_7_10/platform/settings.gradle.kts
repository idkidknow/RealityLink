pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
          name = "GTNH Maven"
          url = uri("https://nexus.gtnewhorizons.com/repository/public/")
          mavenContent {
            includeGroupByRegex("com\\.gtnewhorizons\\..+")
            includeGroup("com.gtnewhorizons")
          }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "realitylink-forge1_7_10-platform"
