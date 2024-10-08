package realcomm

plugins {
    id("java")
    kotlin("jvm")
    id("dev.architectury.loom")
    id("architectury-plugin")
}

group = property("maven_group").toString()
version = property("mod_version").toString()

// https://github.com/gradle/gradle/issues/15383#issuecomment-1940357260
val libs = versionCatalogs.named("libs")

java {
    withSourcesJar()

    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.parchmentmc.org") }
    maven { url = uri("https://maven.neoforged.net/releases") }
}

architectury {
    injectInjectables = false
    minecraft = libs.findVersion("minecraft").get().toString()
}

loom {
    silentMojangMappingsLicense()
}

dependencies {
    minecraft(libs.findLibrary("minecraft").get())
    mappings(loom.layered {
        officialMojangMappings()
        parchment(libs.findLibrary("parchment-mappings").get())
    })
}
