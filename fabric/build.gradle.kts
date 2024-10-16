@file:Suppress("UnstableApiUsage")

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.task.RemapJarTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import realcomm.ModConstant
import realcomm.Versions

plugins {
    id("realcomm.common")
    alias(libs.plugins.fabric.loom)
    alias(libs.plugins.shadow)
}

base {
    archivesName = ModConstant.id
}

loom {
    mixin {
        defaultRefmapName = "${ModConstant.id}.refmap.json"
    }
}

val shadowCommon by configurations.creating

dependencies {
    minecraft("com.mojang:minecraft:${Versions.minecraft}")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-${Versions.minecraft}:${Versions.parchment}@zip")
    })
    modImplementation("net.fabricmc:fabric-loader:${Versions.fabricLoader}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${Versions.fabricApi}")

    implementation(project(path = ":common", configuration = "shadedElements"))
    shadowCommon(project(path = ":common", configuration = "shadedElements"))
}

// Mixins' refmaps are coupled to the compile task
val mixinCodes by configurations.creating
val mixinResources by configurations.creating
dependencies {
    implementation(project(":mixin"))
    mixinCodes(project(path = ":mixin", configuration = "mixinCodes"))
    mixinResources(project(path = ":mixin", configuration = "mixinResources"))
}
tasks.named<ProcessResources>("processResources") {
    dependsOn(mixinResources)
    from(mixinResources)
}
tasks.named<KotlinCompile>("compileKotlin") {
    dependsOn(mixinCodes)
    source(mixinCodes)
}
tasks.named<JavaCompile>("compileJava") {
    dependsOn(mixinCodes)
    source(mixinCodes)
}

val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    configurations = listOf(shadowCommon)
    archiveClassifier = "dev"
}

tasks.named<RemapJarTask>("remapJar") {
    dependsOn(shadowJar)
    inputFile.set(shadowJar.flatMap { it.archiveFile })
}
