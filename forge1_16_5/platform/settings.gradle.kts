pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://files.minecraftforge.net/maven/")
        maven("https://maven.architectury.dev/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "realitylink-forge1_16_5-platform"
