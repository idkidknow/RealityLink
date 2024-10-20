/** Cannot run in dev mode because modMain requires forge mod metadata to be load into the game (what architectury plugin do) */

@file:Suppress("UnstableApiUsage")

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.task.RemapJarTask
import reallink.ModConstant
import reallink.Versions
import reallink.registerGenerateMinimalForgeMetadataAttribute

plugins {
    id("reallink.common")
    id("reallink.platform")
    alias(libs.plugins.architectury.loom)
}

loom {
    silentMojangMappingsLicense()

    forge {
        mixinConfig("${ModConstant.id}.mixins.json")
    }
    mixin {
        useLegacyMixinAp = true
        defaultRefmapName = "${ModConstant.id}.refmap.json"
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${Versions.minecraft}")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-${Versions.minecraft}:${Versions.parchment}@zip")
    })
    "forge"("net.minecraftforge:forge:${Versions.forge}")
}

val shadowModMain by configurations.creating
val generateMetadata = registerGenerateMinimalForgeMetadataAttribute("generateMetadata") {
    modId = "${ModConstant.id}_main"
    packFormat = 6
}
// no jar-in-jar in forge 1.16.5, no kotlin 2 in kotlinforforge (and why it uses meaningless language provider things?)
val shadowKotlin by configurations.creating

dependencies {
    implementation(project(":modMain")) {
        attributes {
            attribute(generateMetadata, true)
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.SHADOWED))
        }
    }
    shadowModMain(project(":modMain")) {
        attributes { attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.SHADOWED)) }
    }
    shadowKotlin(libs.kotlin.stdlib)
}

val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    configurations = listOf(shadowModMain, shadowKotlin)
    relocate("kotlin", "com.idkidknow.mcreallink.shaded.kotlin") {
        exclude("kotlin.Any")
    }
    archiveClassifier = "dev"
}

tasks.named<RemapJarTask>("remapJar") {
    dependsOn(shadowJar)
    inputFile.set(shadowJar.flatMap { it.archiveFile })
}
