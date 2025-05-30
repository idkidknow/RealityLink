pluginManagement {
    repositories {
        mavenCentral()
        maven("https://mcentral.firstdark.dev/releases")
        maven("https://maven.neoforged.net/releases")
        maven("https://maven.minecraftforge.net/")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.wagyourtail.xyz/releases")
        maven("https://maven.wagyourtail.xyz/snapshots")
        gradlePluginPortal {
            content {
                excludeGroup("org.apache.logging.log4j")
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "reallink-neo1211"

includeBuild("../../core") {
    dependencySubstitution {
        substitute(module("com.idkidknow.mcreallink:reallink-core_3")).using(project(":"))
    }
}
include("impl")
