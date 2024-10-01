package realcomm

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.task.RemapJarTask

plugins {
    id("java")
    id("dev.architectury.loom")
    id("architectury-plugin")
    id("com.gradleup.shadow")
}

val platform: String by project

loom {
    mods {
        maybeCreate("main").apply {
            sourceSet(project.sourceSets.main.get())
            sourceSet(project(":common").sourceSets.main.get())
        }
    }
}

architectury {
    platformSetupLoomIde()
}

@Suppress("UnstableApiUsage")
configurations {
    register("shadowBundle") {
        isCanBeResolved = true
        isCanBeConsumed = false
    }
}

dependencies {
    implementation(project(path = ":common", configuration = "namedElements"))
    "shadowBundle"(project(":common"))
}

val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    configurations = listOf(project.configurations["shadowBundle"])
    archiveClassifier = "dev-shadow"
}

tasks.named<RemapJarTask>("remapJar") {
    dependsOn(shadowJar)
    inputFile.set(shadowJar.flatMap { it.archiveFile })
}
