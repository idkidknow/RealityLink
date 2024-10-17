@file:Suppress("UnstableApiUsage")

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.task.RemapJarTask
import realcomm.ModConstant
import realcomm.Versions

plugins {
    id("realcomm.common")
    id("realcomm.platform")
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

val shadowCommon by configurations.creating

dependencies {
    implementation(project(path = ":common", configuration = "shadedElements"))
    shadowCommon(project(path = ":common", configuration = "shadedElements"))
}

val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    configurations = listOf(shadowCommon)
    archiveClassifier = "dev"
}

tasks.named<RemapJarTask>("remapJar") {
    dependsOn(shadowJar)
    inputFile.set(shadowJar.flatMap { it.archiveFile })
}
