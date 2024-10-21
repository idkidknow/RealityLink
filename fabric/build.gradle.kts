@file:Suppress("UnstableApiUsage")

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.task.RemapJarTask
import reallink.ModConstant
import reallink.Versions

plugins {
    id("reallink.common")
    id("reallink.platform")
    alias(libs.plugins.fabric.loom)
}

loom {
    mixin {
        defaultRefmapName = "${ModConstant.id}.refmap.json"
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${Versions.minecraft}")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-${Versions.minecraft}:${Versions.parchment}@zip")
    })
    modImplementation("net.fabricmc:fabric-loader:${Versions.fabricLoader}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${Versions.fabricApi}")
}

val shadowModMain by configurations.creating

dependencies {
    implementation(project(":modMain")) {
        attributes { attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.SHADOWED)) }
    }
    shadowModMain(project(":modMain")) {
        attributes { attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.SHADOWED)) }
    }
}

val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    configurations = listOf(shadowModMain)
    archiveClassifier = "dev"
}

tasks.named<RemapJarTask>("remapJar") {
    dependsOn(shadowJar)
    inputFile.set(shadowJar.flatMap { it.archiveFile })
}
