rootProject.name = "RealityCommunication"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://files.minecraftforge.net/maven/")
        maven("https://maven.fabricmc.net/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

include("modMain")
include("mixin")
include("neoforge")
include("fabric")
