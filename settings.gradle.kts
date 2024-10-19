rootProject.name = "RealityLink"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://files.minecraftforge.net/maven/")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

include("modMain")
include("mixin")
include("forge")
include("fabric")
